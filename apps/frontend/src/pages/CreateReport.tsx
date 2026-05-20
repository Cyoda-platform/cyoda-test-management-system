import { useState, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Breadcrumbs from '@/components/Breadcrumbs';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Checkbox } from '@/components/ui/checkbox';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { PieChart, BarChart3, Bug, Server } from 'lucide-react';
import { toast } from 'sonner';
import { useProject, useTestRuns, useReports, useCreateReport, useSuites, useRepository } from '@/hooks/useApi';
import { useAuth } from '@/contexts/AuthContext';
import { listDisplayId } from '@/lib/utils';
import { testRunCasesApi, defectsApi } from '@/lib/api';
import { computeSnapshotData } from '@/lib/reportSnapshot';

const labelCls = 'text-[10px] font-semibold text-muted-foreground uppercase mb-1.5 block font-mono tracking-widest';

const sectionOptions = [
  { key: 'executiveSummary', label: 'Executive Summary', desc: 'Pie chart (Pass/Fail/Skip) and overall Pass Rate percentage.', icon: PieChart },
  { key: 'suiteAnalytics', label: 'Suite Analytics', desc: 'Bar chart showing results broken down by parent Suites.', icon: BarChart3 },
  { key: 'defectTable', label: 'Detailed Defect Table', desc: 'Table of all linked bugs from the selected runs.', icon: Bug },
  { key: 'environmentInfo', label: 'Environment Info', desc: 'Environment and Build version pulled from selected Test Runs.', icon: Server },
] as const;

const CreateReport = () => {
  const { projectId } = useParams<{ projectId: string }>();
  const navigate = useNavigate();
  const { user }                   = useAuth();
  const { data: project }          = useProject(projectId!);
  const { data: runs = [] }        = useTestRuns(projectId!);
  const { data: existingReports = [] } = useReports(projectId!);
  const createReport               = useCreateReport();
  const { data: suitesList = [] } = useSuites(projectId!);
  const { data: repositoryData }  = useRepository(projectId!);
  const [isBuilding, setIsBuilding] = useState(false);

  const caseToSuiteMap = useMemo(() => {
    const map = new Map<string, string>();
    (repositoryData?.suites ?? []).forEach(s => s.cases.forEach(c => map.set(c.id, s.id)));
    return map;
  }, [repositoryData]);

  const runDisplayIdMap = useMemo(() => {
    const sorted = [...runs].sort((a, b) => (a.createdAt || '').localeCompare(b.createdAt || ''));
    const map: Record<string, string> = {};
    sorted.forEach((r, i) => { map[r.id] = r.displayId || listDisplayId('TR', i); });
    return map;
  }, [runs]);

  const [reportName, setReportName] = useState('');
  const [reportType, setReportType] = useState<string>('Summary');
  const [description, setDescription] = useState('');
  const [selectedRuns, setSelectedRuns] = useState<Set<string>>(new Set());
  const [sections, setSections] = useState({
    executiveSummary: true,
    suiteAnalytics: true,
    defectTable: true,
    environmentInfo: true,
  });

  const toggleRun = (id: string) => {
    setSelectedRuns((prev) => {
      const next = new Set(prev);
      if (next.has(id)) { next.delete(id); } else { next.add(id); }
      return next;
    });
  };

  const toggleSection = (key: string) => {
    setSections((prev) => ({ ...prev, [key]: !prev[key as keyof typeof prev] }));
  };

  const handleCreate = async () => {
    if (!reportName.trim()) {
      toast.error('Report name is required');
      return;
    }

    const effectiveRunIds = selectedRuns.size > 0
      ? Array.from(selectedRuns)
      : runs.map(r => r.id);

    setIsBuilding(true);
    let snapshotDataStr: string | undefined;
    try {
      // Fetches up to 500 cases and 200 defects per run (API defaults).
      // Snapshot is silently truncated for runs that exceed these limits.
      const [runCaseResults, defectResults] = await Promise.all([
        Promise.all(
          effectiveRunIds.map(runId =>
            testRunCasesApi.list(projectId!, runId).catch(() => ({ data: [] })),
          ),
        ),
        Promise.all(
          effectiveRunIds.map(runId =>
            defectsApi.listByRun(projectId!, runId).catch(() => ({ data: [] })),
          ),
        ),
      ]);

      const allRunCases = runCaseResults.flatMap(r => r.data ?? []);
      const allDefects  = defectResults.flatMap(r => r.data ?? []);
      const effectiveRuns = runs.filter(r => effectiveRunIds.includes(r.id));

      const snapshot = computeSnapshotData(allRunCases, effectiveRuns, suitesList, allDefects, caseToSuiteMap);
      snapshotDataStr = JSON.stringify(snapshot);
    } catch {
      // snapshot failed — create report without it rather than blocking the user
    } finally {
      setIsBuilding(false);
    }

    createReport.mutate(
      {
        projectId: projectId!,
        body: {
          name:                    reportName.trim(),
          type:                    reportType as 'Summary' | 'Regression' | 'Sprint' | 'Custom',
          description,
          createdBy:               user?.username || 'unknown',
          selectedRuns:            Array.from(selectedRuns),
          sectionExecutiveSummary: sections.executiveSummary,
          sectionSuiteAnalytics:   sections.suiteAnalytics,
          sectionDefectTable:      sections.defectTable,
          sectionEnvironmentInfo:  sections.environmentInfo,
          ...(snapshotDataStr ? { snapshotData: snapshotDataStr } : {}),
        },
      },
      {
        onSuccess: (report) => {
          toast.success(`Report "${reportName}" created`);
          navigate(`/projects/${projectId}/reports/${report.id}`);
        },
        onError: (e) => toast.error(e.message),
      },
    );
  };

  return (
    <div className="h-full flex flex-col">
      <div className="flex-1 overflow-auto">
        <div className="max-w-5xl mx-auto px-8 py-6 space-y-5">
          <Breadcrumbs segments={[
            { label: 'Projects', href: '/projects' },
            { label: project?.name || 'Project', href: `/projects/${projectId}/repository` },
            { label: 'Reports', href: `/projects/${projectId}/reports` },
            { label: 'Create Report' },
          ]} />

          <h1 className="text-xl font-bold text-foreground tracking-[-0.02em]">Create Report</h1>
          {/* Row 1: Report Name & Report Type */}
          <div className="grid grid-cols-2 gap-5">
            <div>
              <label className={labelCls}>Report Name *</label>
              <Input
                placeholder="e.g., Sprint 12 Summary"
                value={reportName}
                onChange={(e) => setReportName(e.target.value)}
                className="mt-0 h-10 bg-white border border-input focus-visible:ring-1 focus-visible:ring-ring"
              />
            </div>
            <div>
              <label className={labelCls}>Report Type</label>
              <RadioGroup value={reportType} onValueChange={setReportType} className="flex gap-4 mt-1.5 h-10 items-center">
                {['Summary', 'Regression', 'Sprint', 'Custom'].map((t) => (
                  <label key={t} className="flex items-center gap-2 cursor-pointer">
                    <RadioGroupItem value={t} />
                    <span className="text-sm font-medium text-foreground">{t}</span>
                  </label>
                ))}
              </RadioGroup>
            </div>
          </div>

          {/* Description */}
          <div>
            <label className={labelCls}>Description</label>
            <Textarea
              placeholder="Brief description of this report..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={3}
              className="mt-0 bg-white border border-input focus-visible:ring-1 focus-visible:ring-ring resize-none"
            />
          </div>

          {/* Test Runs Selection */}
          <div>
            <label className={labelCls}>Filter by Test Runs</label>
            <div className="border border-border rounded-lg p-3 max-h-56 overflow-auto space-y-1.5 bg-white">
              {runs.length === 0 && <p className="text-sm text-muted-foreground py-2">No test runs found.</p>}
              {runs.map((run) => (
                <label key={run.id} className="flex items-center gap-2.5 py-1.5 px-2 rounded hover:bg-secondary/40 cursor-pointer transition-colors">
                  <Checkbox
                    checked={selectedRuns.has(run.id)}
                    onCheckedChange={() => toggleRun(run.id)}
                  />
                  <span className="font-mono text-[10px] text-accent tracking-wider w-12">{runDisplayIdMap[run.id] ?? '—'}</span>
                  <span className="text-sm text-foreground">{run.name}</span>
                  <span className="ml-auto text-[10px] text-muted-foreground font-mono">{run.environment}</span>
                </label>
              ))}
            </div>
          </div>

          {/* Report Content & Sections */}
          <div>
            <label className={labelCls}>Report Content & Sections</label>
            <p className="text-xs text-muted-foreground mb-1.5">Select which sections to include in the generated report.</p>
            <div className="border border-border rounded-lg overflow-hidden bg-white divide-y divide-border/40">
              {sectionOptions.map(({ key, label, desc, icon: Icon }) => (
                <label key={key} className="flex items-start gap-3 p-3.5 hover:bg-secondary/30 cursor-pointer transition-colors">
                  <Checkbox
                    checked={sections[key as keyof typeof sections]}
                    onCheckedChange={() => toggleSection(key)}
                    className="mt-0.5"
                  />
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <Icon className="h-3.5 w-3.5 text-muted-foreground" strokeWidth={1.5} />
                      <span className="text-sm font-medium text-foreground">{label}</span>
                    </div>
                    <p className="text-xs text-muted-foreground mt-0.5">{desc}</p>
                  </div>
                </label>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* Sticky footer */}
      <div className="shrink-0 border-t border-border bg-card px-8 py-3 flex items-center justify-end gap-3">
        <Button variant="ghost" size="sm" onClick={() => navigate(`/projects/${projectId}/reports`)}>
          Cancel
        </Button>
        <Button
          size="sm"
          className="bg-primary text-primary-foreground hover:bg-primary/90 border-0 px-6"
          onClick={handleCreate}
          disabled={isBuilding || createReport.isPending}
        >
          {isBuilding ? 'Building report…' : createReport.isPending ? 'Creating…' : 'Create Report'}
        </Button>
      </div>
    </div>
  );
};

export default CreateReport;
