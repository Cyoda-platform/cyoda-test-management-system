import { useState, useMemo, useRef, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import Breadcrumbs from '@/components/Breadcrumbs';
import { useQueries } from '@tanstack/react-query';
import { Search, Paperclip, Lock, CheckCircle2, XCircle, MinusCircle, AlertCircle, Upload, FileText, Image, File, Bug, Trash2, ExternalLink, Eye, Pencil, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { toast } from 'sonner';
import CreateDefectModal from '@/components/CreateDefectModal';
import {
  useTestRun,
  useSuites,
  useTestSteps,
  useUpdateTestStep,
  useCompleteTestRun,
  useUnlockTestRun,
  keys,
} from '@/hooks/useApi';
import { testCasesApi, defectsApi, attachmentsApi } from '@/lib/api';
import { isUuid, listDisplayId } from '@/lib/utils';

type StepStatus = 'untested' | 'passed' | 'failed' | 'skipped';

interface EvidenceFile {
  name: string;
  size: number;
  type: string;
  file: File;
}

interface CreatedDefect {
  id: string;
  caseId: string;
  caseTitle: string;
  stepIdx?: number;
  title: string;
  description: string;
  severity: 'Critical' | 'Major' | 'Minor';
  status: 'Open' | 'In Progress' | 'Fixed' | 'Closed';
  link: string;
  files: File[];
  createdAt: string;
}

const severityBadge: Record<string, string> = {
  Critical: 'text-destructive',
  Major: 'text-warning',
  Minor: 'text-accent',
};

const statusBadge: Record<string, string> = {
  Open: 'text-destructive',
  'In Progress': 'text-warning',
  Fixed: 'text-success',
  Closed: 'text-muted-foreground',
};

const statusColors: Record<StepStatus, string> = {
  untested: 'bg-muted text-muted-foreground',
  passed: 'bg-success text-success-foreground ring-1 ring-success/30',
  failed: 'bg-destructive text-destructive-foreground ring-1 ring-destructive/30',
  skipped: 'bg-warning text-warning-foreground ring-1 ring-warning/30',
};

const statusOutline: Record<StepStatus, string> = {
  untested: 'text-muted-foreground border border-border hover:bg-secondary',
  passed: 'text-success border border-success/20 hover:bg-success/10',
  failed: 'text-destructive border border-destructive/20 hover:bg-destructive/10',
  skipped: 'text-warning border border-warning/20 hover:bg-warning/10',
};

const caseStatusIcon: Record<string, string> = {
  untested: 'bg-muted-foreground/30',
  passed: 'bg-success',
  failed: 'bg-destructive',
  skipped: 'bg-warning',
};

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function getFileIcon(type: string) {
  if (type.startsWith('image/')) return Image;
  if (type.includes('pdf') || type.includes('document') || type.includes('text')) return FileText;
  return File;
}

const RunExecution = () => {
  const { projectId, runId } = useParams<{ projectId: string; runId: string }>();

  // Live API data
  const { data: run, isLoading: runLoading } = useTestRun(projectId!, runId!);
  const { data: suites = [], isLoading: suitesLoading } = useSuites(projectId!);

  // Fetch cases for every suite in parallel
  const casesQueries = useQueries({
    queries: suites.map((suite) => ({
      queryKey: keys.cases.all(projectId!, suite.id),
      queryFn:  () => testCasesApi.list(projectId!, suite.id),
      enabled:  !!projectId && suites.length > 0,
      select:   (res: { data: Array<{ id: string; displayId?: string; shortId?: string; projectId: string; suiteId: string; title: string; priority: 'HIGH' | 'MEDIUM' | 'LOW'; description: string; preconditions: string; deleted: boolean }> }) => res.data,
    })),
  });

  const suitesWithCases = suites.map((suite, i) => ({
    ...suite,
    cases: casesQueries[i]?.data ?? [],
  }));

  const allCases = suitesWithCases.flatMap((s) => s.cases);

  /** Returns a human-readable source label for a given case ID. */
  const getCaseSourceLabel = (caseId: string): string => {
    for (const suite of suitesWithCases) {
      const idx = suite.cases.findIndex((c) => c.id === caseId);
      if (idx !== -1) {
        const c = suite.cases[idx];
        if (c.displayId?.trim()) return c.displayId.trim();
        if (c.shortId?.trim()) return c.shortId.trim();
        // Derive prefix from suite name (same logic as Repository.tsx)
        const words = suite.name.match(/[A-Za-z0-9]+/g) ?? [];
        let prefix = 'TC';
        if (words.length > 0) {
          const acronym = words.find((w) => /^[A-Z0-9]{2,4}$/.test(w));
          if (acronym) prefix = acronym.slice(0, 4);
          else if (words.length > 1) prefix = words.slice(0, 3).map((w) => w[0]).join('').toUpperCase();
          else prefix = words[0].slice(0, 3).toUpperCase();
        }
        return `${prefix}-${idx + 1}`;
      }
    }
    return isUuid(caseId) ? caseId.slice(0, 8) : caseId;
  };

  // Mutations
  const updateStep     = useUpdateTestStep();
  const completeRun    = useCompleteTestRun();
  const unlockRun      = useUnlockTestRun();
  const [selectedIdx, setSelectedIdx] = useState(0);
  const [stepStatuses, setStepStatuses] = useState<Record<string, StepStatus[]>>({});
  const [stepEvidence, setStepEvidence] = useState<Record<string, EvidenceFile[]>>({});
  const [evidenceModalOpen, setEvidenceModalOpen] = useState(false);
  const [evidenceTarget, setEvidenceTarget] = useState<{ caseId: string; stepIdx: number } | null>(null);
  const [defectModalOpen, setDefectModalOpen] = useState(false);
  const [defectContext, setDefectContext] = useState<{ caseId: string; stepIdx?: number; source: string } | null>(null);
  const [createdDefects, setCreatedDefects] = useState<CreatedDefect[]>([]);
  const [viewDefectOpen, setViewDefectOpen] = useState(false);
  const [viewDefect, setViewDefect] = useState<CreatedDefect | null>(null);
  const [editDefectOpen, setEditDefectOpen] = useState(false);
  const [editDefect, setEditDefect] = useState<CreatedDefect | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const isReadOnly = run?.status === 'completed';

  // Active case and its live steps
  const activeCase = allCases[selectedIdx];
  const { data: steps = [] } = useTestSteps(
    projectId!,
    activeCase?.suiteId ?? '',
    activeCase?.id ?? '',
  );

  // Sort steps by stepNumber to ensure consistent ordering
  const sortedSteps = useMemo(() =>
    [...steps].sort((a, b) => (a.stepNumber || 0) - (b.stepNumber || 0)),
    [steps]
  );

  // Initialise local step-status state from API data when a new case is loaded
  useEffect(() => {
    if (sortedSteps.length > 0 && activeCase) {
      setStepStatuses((prev) => {
        if (prev[activeCase.id]) return prev; // already initialised — keep user changes
        const initial: StepStatus[] = sortedSteps.map((s) => {
          const v = s.status as StepStatus;
          return ['passed', 'failed', 'skipped'].includes(v) ? v : 'untested';
        });
        return { ...prev, [activeCase.id]: initial };
      });
    }
  }, [sortedSteps, activeCase?.id]);

  const getStepStatuses = (caseId: string): StepStatus[] => {
    return stepStatuses[caseId] || [];
  };

  const setStepStatus = (caseId: string, stepNumber: number, stepId: string, status: StepStatus) => {
    if (isReadOnly) return;
    const current = stepStatuses[caseId] || Array(sortedSteps.length).fill('untested');
    const updated = [...current];
    updated[stepNumber - 1] = status; // stepNumber is 1-based, array is 0-based
    setStepStatuses({ ...stepStatuses, [caseId]: updated });

    // Persist to API
    if (stepId && activeCase) {
      updateStep.mutate({
        projectId: projectId!,
        suiteId:   activeCase.suiteId,
        caseId,
        id:        stepId,
        body:      { status },
      });
    }

    // Auto-trigger defect modal on 'failed'
    if (status === 'failed') {
      setDefectContext({ caseId, stepIdx: stepNumber - 1, source: getCaseSourceLabel(caseId) });
      setDefectModalOpen(true);
    }
  };

  const getCaseStatus = (caseId: string): string => {
    const statuses = stepStatuses[caseId];
    if (!statuses || statuses.length === 0) return 'untested';
    if (statuses.some((s) => s === 'failed')) return 'failed';
    if (statuses.every((s) => s === 'passed')) return 'passed';
    if (statuses.some((s) => s === 'skipped') && !statuses.some((s) => s === 'untested')) return 'skipped';
    return 'untested';
  };

  const getEvidenceKey = (caseId: string, stepIdx: number) => `${caseId}::${stepIdx}`;

  const getEvidenceFiles = (caseId: string, stepIdx: number): EvidenceFile[] => {
    return stepEvidence[getEvidenceKey(caseId, stepIdx)] || [];
  };

  const getCaseAttachments = (caseId: string): Array<{ file: EvidenceFile; stepIdx: number }> => {
    const attachments: Array<{ file: EvidenceFile; stepIdx: number }> = [];
    Object.entries(stepEvidence).forEach(([key, files]) => {
      const [kcaseId, stepIdxStr] = key.split('::');
      if (kcaseId === caseId) {
        const stepIdx = parseInt(stepIdxStr);
        files.forEach(f => attachments.push({ file: f, stepIdx }));
      }
    });
    return attachments;
  };

  const handleEvidenceClick = (caseId: string, stepIdx: number) => {
    if (isReadOnly) return;
    setEvidenceTarget({ caseId, stepIdx });
    setEvidenceModalOpen(true);
  };

  const handleBugClick = (caseId: string, stepIdx?: number) => {
    if (isReadOnly) return;
    setDefectContext({ caseId, stepIdx, source: getCaseSourceLabel(caseId) });
    setDefectModalOpen(true);
  };

  const handleFileUpload = async (files: FileList | null) => {
    if (!files || !evidenceTarget) return;
    const key = getEvidenceKey(evidenceTarget.caseId, evidenceTarget.stepIdx);
    const fileArray = Array.from(files);
    const newFiles: EvidenceFile[] = fileArray.map((f) => ({
      name: f.name,
      size: f.size,
      type: f.type,
      file: f,
    }));
    setStepEvidence((prev) => ({
      ...prev,
      [key]: [...(prev[key] || []), ...newFiles],
    }));
    try {
      await Promise.all(
        fileArray.map((f) => attachmentsApi.upload(projectId!, f, evidenceTarget.caseId))
      );
      toast.success(`${fileArray.length} file(s) uploaded`);
    } catch {
      toast.error('Upload failed — files saved locally but not persisted');
    }
  };

  const handleCreateDefect = async (defect: any) => {
    if (!defectContext) return;
    try {
      const created = await defectsApi.create(projectId!, {
        title: defect.title,
        description: defect.description,
        severity: defect.severity,
        status: defect.status,
        source: defect.source,
        link: defect.link,
      });

      // Upload any attached files, linked to the test case
      if (defect.files && (defect.files as File[]).length > 0) {
        await Promise.all(
          (defect.files as File[]).map((f) =>
            attachmentsApi.upload(projectId!, f, defect.caseId)
          )
        );
      }

      const newDefect: CreatedDefect = {
        id: (created as any).id ?? `DEF-${Date.now()}`,
        caseId: defect.caseId,
        caseTitle: activeCase.title,
        stepIdx: defect.stepIdx,
        title: defect.title,
        description: defect.description,
        severity: defect.severity,
        status: defect.status,
        link: defect.link,
        files: defect.files,
        createdAt: new Date().toISOString().split('T')[0],
      };
      setCreatedDefects([...createdDefects, newDefect]);
      setDefectModalOpen(false);
      toast.success('Defect created');
    } catch {
      toast.error('Failed to create defect — please try again');
    }
  };

  const progressStats = useMemo(() => {
    let passed = 0, failed = 0, skipped = 0, untested = 0;
    allCases.forEach((tc) => {
      const cStatus = getCaseStatus(tc.id);
      if (cStatus === 'passed') passed++;
      else if (cStatus === 'failed') failed++;
      else if (cStatus === 'skipped') skipped++;
      else untested++;
    });
    const total = allCases.length;
    const completed = passed + failed + skipped;
    return { passed, failed, skipped, untested, total, completed };
  }, [stepStatuses, allCases]);

  // Loading / empty guard
  if (runLoading || suitesLoading) {
    return (
      <div className="flex items-center justify-center h-full gap-2 text-muted-foreground">
        <Loader2 className="h-5 w-5 animate-spin" />
        <span className="text-sm">Loading run…</span>
      </div>
    );
  }

  if (!run) return <div className="p-8 text-muted-foreground">Run not found</div>;

  // While cases are still loading show a minimal placeholder body
  if (!activeCase && casesQueries.some((q) => q.isLoading)) {
    return (
      <div className="flex items-center justify-center h-full gap-2 text-muted-foreground">
        <Loader2 className="h-5 w-5 animate-spin" />
        <span className="text-sm">Loading test cases…</span>
      </div>
    );
}

  if (!activeCase) return <div className="p-8 text-muted-foreground">No test cases found for this run.</div>;

  const { passed, failed, skipped, untested, total, completed } = progressStats;

  return (
    <div className="h-full flex flex-col">
      {/* Execution Header */}
      <div className="px-6 py-4 bg-card border-b border-border">
        <div className="mb-2">
          <Breadcrumbs segments={[
            { label: 'Projects', href: '/projects' },
            { label: run.projectId ? 'Project' : 'Project', href: `/projects/${projectId}/repository` },
            { label: 'Test Runs', href: `/projects/${projectId}/runs` },
            { label: run.name || 'Run' },
          ]} />
        </div>

        <div className="flex items-center gap-4 mt-3">
          <span className="text-[10px] px-2 py-0.5 rounded-md bg-secondary text-muted-foreground font-mono uppercase tracking-widest">{run.environment}</span>

          <div className="flex-1 max-w-md">
            <div className="flex h-3 rounded-full overflow-hidden w-full bg-secondary">
              {passed > 0 && <div className="bg-success transition-all" style={{ width: `${(passed / total) * 100}%` }} />}
              {failed > 0 && <div className="bg-destructive transition-all" style={{ width: `${(failed / total) * 100}%` }} />}
              {skipped > 0 && <div className="bg-warning transition-all" style={{ width: `${(skipped / total) * 100}%` }} />}
              {untested > 0 && <div className="bg-muted-foreground/20 transition-all" style={{ width: `${(untested / total) * 100}%` }} />}
            </div>
          </div>

          <span className="text-sm font-semibold text-foreground whitespace-nowrap">
            {completed} of {total} cases completed
          </span>

          <div className="flex items-center gap-3 ml-2">
            <div className="flex items-center gap-1">
              <CheckCircle2 className="h-3.5 w-3.5 text-success" strokeWidth={2} />
              <span className="text-xs font-mono font-semibold text-success">{passed}</span>
            </div>
            <div className="flex items-center gap-1">
              <XCircle className="h-3.5 w-3.5 text-destructive" strokeWidth={2} />
              <span className="text-xs font-mono font-semibold text-destructive">{failed}</span>
            </div>
            <div className="flex items-center gap-1">
              <AlertCircle className="h-3.5 w-3.5 text-warning" strokeWidth={2} />
              <span className="text-xs font-mono font-semibold text-warning">{skipped}</span>
            </div>
            <div className="flex items-center gap-1">
              <MinusCircle className="h-3.5 w-3.5 text-muted-foreground" strokeWidth={2} />
              <span className="text-xs font-mono font-semibold text-muted-foreground">{untested}</span>
            </div>
          </div>

          {isReadOnly ? (
            <Button
              variant="ghost"
              size="sm"
              className="gap-1.5 ml-auto"
              disabled={unlockRun.isPending}
              onClick={() =>
                unlockRun.mutate(
                  { projectId: projectId!, id: runId! },
                  {
                    onSuccess: () => toast.success('Run unlocked'),
                    onError:   (e) => toast.error(e.message),
                  }
                )
              }
            >
              <Lock className="h-3.5 w-3.5" strokeWidth={1.5} />
              {unlockRun.isPending ? 'Unlocking…' : 'Unlock Run'}
            </Button>
          ) : (
            <Button
              size="sm"
              className="gap-1.5 ml-auto bg-gradient-to-br from-primary to-primary/80 text-primary-foreground border-0"
              disabled={completeRun.isPending}
              onClick={() =>
                completeRun.mutate(
                  { projectId: projectId!, id: runId! },
                  {
                    onSuccess: () => toast.success('Run completed'),
                    onError:   (e) => toast.error(e.message),
                  }
                )
              }
            >
              {completeRun.isPending ? <><Loader2 className="h-3.5 w-3.5 animate-spin" /> Completing…</> : 'Complete Run'}
            </Button>
          )}
        </div>
      </div>

      <div className="flex flex-1 overflow-hidden">
        {/* Left: Case List Grouped by Suite */}
        <div className="w-64 surface-low overflow-auto shrink-0">
          <div className="p-3">
            <div className="relative">
              <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground" strokeWidth={1.5} />
              <Input placeholder="Find case..." className="pl-8 h-8 text-xs bg-card border-0 focus-visible:ring-1 focus-visible:ring-accent/40" />
            </div>
          </div>
          <div className="px-1.5 pb-2">
            {suitesWithCases.map((suite) => (
              <div key={suite.id} className="mb-1">
                <div className="px-3 py-2 text-[10px] font-semibold text-muted-foreground uppercase tracking-widest bg-muted/40 rounded-md mb-0.5">
                  {suite.name}
                </div>
                {suite.cases.map((tc, index) => {
                  const globalIdx = allCases.findIndex((c) => c.id === tc.id);
                  const cStatus = getCaseStatus(tc.id);
                  return (
                    <button
                      key={tc.id}
                      onClick={() => setSelectedIdx(globalIdx)}
                      className={`w-full flex items-center gap-2.5 px-3 py-2.5 rounded-md text-sm transition-colors ${
                        globalIdx === selectedIdx ? 'bg-card shadow-soft' : 'hover:bg-card/60'
                      }`}
                    >
                      <div className={`h-2.5 w-2.5 rounded-full shrink-0 ${caseStatusIcon[cStatus]}`} />
                      <div className="flex-1 text-left min-w-0">
                        <span className="text-[10px] text-muted-foreground font-mono tracking-wider" title={tc.id}>
                          {listDisplayId('TC', index)}
                        </span>
                        <p className="text-xs font-medium text-foreground truncate">{tc.title}</p>
                      </div>
                    </button>
                  );
                })}
              </div>
            ))}
          </div>
        </div>

        {/* Center: Step Execution */}
        <div className="flex-1 overflow-auto p-6 bg-card">
          <div className="mb-5 flex items-start justify-between">
            <div>
              <span className="text-[10px] font-mono text-muted-foreground tracking-wider">{activeCase.id}</span>
              <div className="flex items-center gap-3 mt-1">
                <h2 className="text-lg font-bold text-foreground tracking-[-0.02em]">{activeCase.title}</h2>
                <span className={`inline-block px-2.5 py-0.5 text-[10px] uppercase tracking-widest font-mono ${
                  activeCase.priority === 'HIGH' ? 'text-destructive' :
                  activeCase.priority === 'MEDIUM' ? 'text-warning' :
                  'text-muted-foreground'
                }`}>
                  {activeCase.priority}
                </span>
              </div>
            </div>
            {!isReadOnly && (
              <Button
                variant="outline"
                size="sm"
                className="gap-1.5 text-destructive border-destructive/20 hover:bg-destructive/10 hover:text-destructive shrink-0"
                onClick={() => handleBugClick(activeCase.id)}
              >
                <Bug className="h-3.5 w-3.5" strokeWidth={1.5} />
                Report Case Issue
              </Button>
            )}
          </div>

          {/* Case Description and Pre-conditions */}
          <div className="grid grid-cols-2 gap-4 mb-6">
            {activeCase.description && (
              <div className="bg-muted/40 rounded-lg p-4">
                <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-widest mb-2">Description</h3>
                <p className="text-sm text-foreground">{activeCase.description}</p>
              </div>
            )}
            {activeCase.preconditions && (
              <div className="bg-muted/40 rounded-lg p-4">
                <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-widest mb-2">Pre-conditions</h3>
                <p className="text-sm text-foreground">{activeCase.preconditions}</p>
              </div>
            )}
          </div>

          {/* Attachments Section */}
          {(() => {
            const caseAttachments = getCaseAttachments(activeCase.id);
            if (caseAttachments.length === 0) return null;
            return (
              <div className="bg-muted/40 rounded-lg p-4 mb-6">
                <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-widest mb-3">Attachments</h3>
                <div className="space-y-2">
                  {caseAttachments.map((item, idx) => {
                    const IconComp = getFileIcon(item.file.type);
                    return (
                      <div key={idx} className="flex items-center gap-2.5 px-3 py-2 bg-card rounded-md border border-border/40">
                        <IconComp className="h-4 w-4 text-muted-foreground shrink-0" strokeWidth={1.5} />
                        <div className="flex-1 min-w-0">
                          <p className="text-sm text-foreground truncate">{item.file.name}</p>
                          <span className="text-[10px] text-muted-foreground">Step {item.stepIdx + 1}</span>
                        </div>
                        <span className="text-xs text-muted-foreground shrink-0">{formatFileSize(item.file.size)}</span>
                        {!isReadOnly && (
                          <button
                            onClick={() => {
                              const key = getEvidenceKey(activeCase.id, item.stepIdx);
                              setStepEvidence(prev => ({
                                ...prev,
                                [key]: (prev[key] || []).filter(f => f.name !== item.file.name)
                              }));
                              toast.success('File removed');
                            }}
                            className="p-1 rounded hover:bg-muted text-muted-foreground hover:text-destructive transition-colors shrink-0"
                          >
                            <Trash2 className="h-3.5 w-3.5" strokeWidth={1.5} />
                          </button>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
            );
          })()}

          {steps.length > 0 ? (
            <div className="bg-card rounded-lg shadow-soft overflow-hidden">
              <table className="w-full text-sm">
                <thead>
                  <tr className="surface-low">
                    <th className="px-4 py-2.5 text-left font-medium text-muted-foreground text-xs uppercase tracking-wider w-10">#</th>
                    <th className="px-4 py-2.5 text-left font-medium text-muted-foreground text-xs uppercase tracking-wider">Step</th>
                    <th className="px-4 py-2.5 text-left font-medium text-muted-foreground text-xs uppercase tracking-wider">Expected result</th>
                    <th className="px-4 py-2.5 text-center font-medium text-muted-foreground text-xs uppercase tracking-wider w-56">Status</th>
                    <th className="px-4 py-2.5 text-center font-medium text-muted-foreground text-xs uppercase tracking-wider w-24">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {sortedSteps.map((step, sIdx) => {
                    const currentStatus = (stepStatuses[activeCase.id] || [])[sIdx] ?? 'untested';
                    const evidenceFiles = getEvidenceFiles(activeCase.id, sIdx);
                    return (
                      <tr key={sIdx} className="ghost-border border-t">
                        <td className="px-4 py-3 text-muted-foreground font-mono text-xs">{step.stepNumber}</td>
                        <td className="px-4 py-3 text-foreground">{step.action}</td>
                        <td className="px-4 py-3 text-foreground">{step.expectedResult}</td>
                        <td className="px-4 py-3">
                          <div className="flex items-center justify-center gap-1">
                            {(['untested', 'passed', 'failed', 'skipped'] as StepStatus[]).map((s) => (
                              <button
                                key={s}
                                onClick={() => setStepStatus(activeCase.id, step.stepNumber, step.id, s)}
                                disabled={isReadOnly}
                                className={`px-2.5 py-1 rounded-md text-[10px] font-mono font-medium uppercase tracking-widest transition-colors ${
                                  currentStatus === s ? statusColors[s] : statusOutline[s]
                                } ${isReadOnly ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}`}
                              >
                                {s}
                              </button>
                            ))}
                          </div>
                        </td>
                        <td className="px-4 py-3">
                          <div className="flex items-center justify-center gap-1">
                            {/* Paperclip - Evidence */}
                            <div className="relative">
                              <Button
                                variant="ghost"
                                size="icon"
                                className={`h-7 w-7 ${evidenceFiles.length > 0 ? 'text-primary' : 'text-muted-foreground'} ${isReadOnly ? 'opacity-50 cursor-not-allowed' : ''}`}
                                onClick={() => handleEvidenceClick(activeCase.id, step.stepNumber - 1)}
                                disabled={isReadOnly}
                              >
                                <Paperclip className="h-3.5 w-3.5" strokeWidth={1.5} />
                              </Button>
                              {evidenceFiles.length > 0 && (
                                <span className="absolute -top-1 -right-1 h-4 w-4 rounded-full bg-primary text-primary-foreground text-[9px] font-bold flex items-center justify-center">
                                  {evidenceFiles.length}
                                </span>
                              )}
                            </div>
                            {/* Bug - Defect */}
                            <Button
                              variant="ghost"
                              size="icon"
                              className={`h-7 w-7 text-muted-foreground hover:text-destructive ${isReadOnly ? 'opacity-50 cursor-not-allowed' : ''}`}
                              onClick={() => handleBugClick(activeCase.id, step.stepNumber - 1)}
                              disabled={isReadOnly}
                            >
                              <Bug className="h-3.5 w-3.5" strokeWidth={1.5} />
                            </Button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="text-center py-12 text-muted-foreground text-sm">No steps defined for this case.</div>
          )}

          {/* Defects Section - Inside center panel */}
          {createdDefects.length > 0 && (
            <div className="mt-6 bg-card rounded-lg shadow-soft overflow-hidden">
              <table className="w-full text-sm">
                <thead>
                  <tr className="bg-slate-200 dark:bg-slate-700 sticky top-0 z-10">
                    <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">ID</th>
                    <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Title</th>
                    <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Severity</th>
                    <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Status</th>
                    <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Source</th>
                    <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Created</th>
                    <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {createdDefects.map((d) => (
                    <tr
                      key={d.id}
                      className="hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-colors border-b border-slate-100 dark:border-slate-700/50 bg-card cursor-pointer"
                      onClick={() => { setViewDefect(d); setViewDefectOpen(true); }}
                    >
                      <td className="px-5 py-3.5 font-mono text-[10px] text-accent tracking-wider">{d.id}</td>
                      <td className="px-5 py-3.5 font-medium text-foreground">{d.title}</td>
                      <td className="px-5 py-3.5" onClick={(e) => e.stopPropagation()}>
                        <Select value={d.severity} onValueChange={(val) => {
                          const updated = createdDefects.map(x => x.id === d.id ? { ...x, severity: val as any } : x);
                          setCreatedDefects(updated);
                        }}>
                          <SelectTrigger className={`h-7 w-auto border-0 text-[10px] font-semibold font-mono uppercase tracking-widest rounded-md px-2.5 ${severityBadge[d.severity] || ''}`}>
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="Critical">Critical</SelectItem>
                            <SelectItem value="Major">Major</SelectItem>
                            <SelectItem value="Minor">Minor</SelectItem>
                          </SelectContent>
                        </Select>
                      </td>
                      <td className="px-5 py-3.5" onClick={(e) => e.stopPropagation()}>
                        <Select value={d.status} onValueChange={(val) => {
                          const updated = createdDefects.map(x => x.id === d.id ? { ...x, status: val as any } : x);
                          setCreatedDefects(updated);
                        }}>
                          <SelectTrigger className={`h-7 w-auto text-[10px] font-mono uppercase tracking-widest rounded-md px-2.5 ${statusBadge[d.status] || ''}`}>
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="Open">Open</SelectItem>
                            <SelectItem value="In Progress">In Progress</SelectItem>
                            <SelectItem value="Fixed">Fixed</SelectItem>
                            <SelectItem value="Closed">Closed</SelectItem>
                          </SelectContent>
                        </Select>
                      </td>
                      <td className="px-5 py-3.5 font-mono text-[10px] text-muted-foreground tracking-wider" title={d.stepIdx === undefined ? d.caseId : ''}>
                        {d.stepIdx !== undefined ? `Step ${d.stepIdx + 1}` : getCaseSourceLabel(d.caseId)}
                      </td>
                      <td className="px-5 py-3.5 text-muted-foreground font-mono text-[10px] tracking-wider">{d.createdAt}</td>
                      <td className="px-5 py-3.5" onClick={(e) => e.stopPropagation()}>
                        <div className="flex items-center gap-1">
                          <button
                            onClick={() => { setViewDefect(d); setViewDefectOpen(true); }}
                            className="p-1.5 rounded hover:bg-muted text-muted-foreground hover:text-foreground transition-colors"
                          >
                            <Eye className="h-3.5 w-3.5" strokeWidth={1.5} />
                          </button>
                          <button
                            onClick={() => { setEditDefect(d); setEditDefectOpen(true); }}
                            className="p-1.5 rounded hover:bg-muted text-muted-foreground hover:text-foreground transition-colors"
                          >
                            <Pencil className="h-3.5 w-3.5" strokeWidth={1.5} />
                          </button>
                          <button
                            onClick={() => {
                              setCreatedDefects(createdDefects.filter(x => x.id !== d.id));
                              toast.success('Defect deleted');
                            }}
                            className="p-1.5 rounded hover:bg-muted text-muted-foreground hover:text-destructive transition-colors"
                          >
                            <Trash2 className="h-3.5 w-3.5" strokeWidth={1.5} />
                          </button>
                          {d.link && (
                            <a
                              href={d.link}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="p-1.5 rounded hover:bg-muted text-muted-foreground hover:text-foreground transition-colors"
                            >
                              <ExternalLink className="h-3.5 w-3.5" strokeWidth={1.5} />
                            </a>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* Evidence Upload Modal */}
      <Dialog open={evidenceModalOpen} onOpenChange={setEvidenceModalOpen}>
        <DialogContent className="sm:max-w-md bg-card">
          <DialogHeader>
            <DialogTitle className="text-foreground">Upload Evidence</DialogTitle>
            <DialogDescription className="text-muted-foreground">
              Attach screenshots, logs, or files for{' '}
              {evidenceTarget ? `Step ${(evidenceTarget.stepIdx + 1)}` : ''} of case{' '}
              {evidenceTarget ? getCaseSourceLabel(evidenceTarget.caseId) : ''}.
            </DialogDescription>
          </DialogHeader>

          <div
            className="border-2 border-dashed border-border rounded-lg p-8 text-center cursor-pointer hover:border-primary/50 hover:bg-accent/5 transition-colors"
            onClick={() => fileInputRef.current?.click()}
            onDragOver={(e) => { e.preventDefault(); e.stopPropagation(); }}
            onDrop={(e) => {
              e.preventDefault();
              e.stopPropagation();
              handleFileUpload(e.dataTransfer.files);
            }}
          >
            <Upload className="h-8 w-8 text-muted-foreground mx-auto mb-3" strokeWidth={1.5} />
            <p className="text-sm text-foreground font-medium mb-1">Drop files here or click to browse</p>
            <p className="text-xs text-muted-foreground">Screenshots, logs, PDFs, or any relevant files</p>
            <input
              ref={fileInputRef}
              type="file"
              multiple
              className="hidden"
              onChange={(e) => handleFileUpload(e.target.files)}
            />
          </div>

          {evidenceTarget && getEvidenceFiles(evidenceTarget.caseId, evidenceTarget.stepIdx).length > 0 && (
            <div className="mt-3 space-y-1.5">
              <p className="text-xs font-semibold text-foreground uppercase tracking-wider">Uploaded Files</p>
              {getEvidenceFiles(evidenceTarget.caseId, evidenceTarget.stepIdx).map((file, i) => {
                const IconComp = getFileIcon(file.type);
                return (
                  <div key={i} className="flex items-center gap-2.5 px-3 py-2 bg-secondary rounded-md">
                    <IconComp className="h-4 w-4 text-muted-foreground shrink-0" strokeWidth={1.5} />
                    <span className="text-sm text-foreground truncate flex-1">{file.name}</span>
                    <span className="text-xs text-muted-foreground shrink-0">{formatFileSize(file.size)}</span>
                  </div>
                );
              })}
            </div>
          )}

          <div className="flex justify-end mt-4">
            <Button variant="ghost" onClick={() => setEvidenceModalOpen(false)}>Done</Button>
          </div>
        </DialogContent>
      </Dialog>

      {/* Create Defect Modal */}
      {defectContext && (
        <CreateDefectModal
          open={defectModalOpen}
          onOpenChange={setDefectModalOpen}
          caseId={defectContext.caseId}
          caseTitle={activeCase.title}
          stepIdx={defectContext.stepIdx}
          sourceInitial={defectContext.source}
          onCreateDefect={handleCreateDefect}
        />
      )}

      {/* View Defect Modal */}
      <Dialog open={viewDefectOpen} onOpenChange={setViewDefectOpen}>
        <DialogContent className="sm:max-w-lg bg-card">
          <DialogHeader>
            <DialogTitle className="text-foreground flex items-center gap-2">
              <span className="font-mono text-purple-600 dark:text-purple-400 text-sm">{viewDefect?.id}</span>
              {viewDefect?.title}
            </DialogTitle>
            <DialogDescription className="text-muted-foreground">Defect details</DialogDescription>
          </DialogHeader>
          {viewDefect && (
            <div className="space-y-4">
              <div className="flex items-center gap-2">
                <span className={`inline-block px-2 py-0.5 text-[10px] uppercase tracking-wider font-mono ${severityBadge[viewDefect.severity] || 'text-muted-foreground'}`}>
                  {viewDefect.severity}
                </span>
                <span className={`inline-block px-2 py-0.5 text-[10px] uppercase tracking-wider font-mono ${statusBadge[viewDefect.status] || 'text-muted-foreground'}`}>
                  {viewDefect.status}
                </span>
              </div>
              <div>
                <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-1">Description</p>
                <p className="text-sm text-foreground">{viewDefect.description}</p>
              </div>
              <div>
                <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-1">Case</p>
                <p className="text-sm text-foreground">{viewDefect.caseTitle}</p>
              </div>
              {viewDefect.stepIdx !== undefined && (
                <div>
                  <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-1">Step</p>
                  <p className="text-sm text-foreground">Step {viewDefect.stepIdx + 1}</p>
                </div>
              )}
              {viewDefect.link && (
                <div>
                  <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-1">External Link</p>
                  <a href={viewDefect.link} target="_blank" rel="noopener noreferrer" className="text-sm text-accent hover:underline">
                    {viewDefect.link}
                  </a>
                </div>
              )}
              <div>
                <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-1">Created</p>
                <p className="text-sm text-foreground">{viewDefect.createdAt}</p>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Edit Defect Modal */}
      <Dialog open={editDefectOpen} onOpenChange={setEditDefectOpen}>
        <DialogContent className="sm:max-w-lg bg-card">
          <DialogHeader>
            <DialogTitle className="text-foreground">Edit Defect</DialogTitle>
            <DialogDescription className="text-muted-foreground">{editDefect?.id}</DialogDescription>
          </DialogHeader>
          {editDefect && (
            <div className="space-y-4">
              <div>
                <label className="text-xs font-medium text-muted-foreground uppercase tracking-wider">Title</label>
                <input
                  type="text"
                  value={editDefect.title}
                  onChange={(e) => setEditDefect({ ...editDefect, title: e.target.value })}
                  className="mt-1.5 w-full h-9 px-3 bg-secondary border-0 rounded-md text-sm text-foreground"
                />
              </div>
              <div>
                <label className="text-xs font-medium text-muted-foreground uppercase tracking-wider">Description</label>
                <Textarea
                  value={editDefect.description}
                  onChange={(e) => setEditDefect({ ...editDefect, description: e.target.value })}
                  className="mt-1.5 min-h-[80px] bg-secondary border-0 text-sm"
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-medium text-muted-foreground uppercase tracking-wider">Severity</label>
                  <Select value={editDefect.severity} onValueChange={(val) => setEditDefect({ ...editDefect, severity: val as any })}>
                    <SelectTrigger className="mt-1.5 h-9 bg-secondary border-0"><SelectValue /></SelectTrigger>
                    <SelectContent>
                      <SelectItem value="Critical">Critical</SelectItem>
                      <SelectItem value="Major">Major</SelectItem>
                      <SelectItem value="Minor">Minor</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div>
                  <label className="text-xs font-medium text-muted-foreground uppercase tracking-wider">Status</label>
                  <Select value={editDefect.status} onValueChange={(val) => setEditDefect({ ...editDefect, status: val as any })}>
                    <SelectTrigger className="mt-1.5 h-9 bg-secondary border-0"><SelectValue /></SelectTrigger>
                    <SelectContent>
                      <SelectItem value="Open">Open</SelectItem>
                      <SelectItem value="In Progress">In Progress</SelectItem>
                      <SelectItem value="Fixed">Fixed</SelectItem>
                      <SelectItem value="Closed">Closed</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>
              <div>
                <label className="text-xs font-medium text-muted-foreground uppercase tracking-wider">External Link</label>
                <input
                  type="text"
                  value={editDefect.link}
                  onChange={(e) => setEditDefect({ ...editDefect, link: e.target.value })}
                  className="mt-1.5 w-full h-9 px-3 bg-secondary border-0 rounded-md text-sm text-foreground"
                />
              </div>
              <div className="flex justify-end gap-2 pt-4">
                <Button variant="ghost" onClick={() => setEditDefectOpen(false)}>Cancel</Button>
                <Button onClick={() => { toast.success('Defect updated'); setEditDefectOpen(false); }}>Save Changes</Button>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default RunExecution;
