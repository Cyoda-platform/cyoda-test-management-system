import { useParams, useNavigate } from 'react-router-dom';
import Breadcrumbs from '@/components/Breadcrumbs';
import { Button } from '@/components/ui/button';
import { ArrowLeft, Download, ExternalLink, Trash2, AlertTriangle, Eye, Pencil, Loader2 } from 'lucide-react';
import { useProject, useTestRuns, useDefects, useUpdateDefect, useDeleteDefect, useSuites, useReport, keys } from '@/hooks/useApi';
import type { Defect } from '@/lib/api';
import { testCasesApi, testStepsApi, attachmentsApi } from '@/lib/api';
import { listDisplayId, formatDate, isUuid } from '@/lib/utils';
import { useMemo, useState } from 'react';
import { useQueries } from '@tanstack/react-query';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@/components/ui/dialog';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import {
  PieChart, Pie, Cell, ResponsiveContainer, Tooltip,
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Legend,
} from 'recharts';
import { toast } from 'sonner';
import ViewDefectModal from '@/components/ViewDefectModal';
import EditDefectModal from '@/components/EditDefectModal';

import type { Report } from '@/lib/api';

const typeBadge: Record<string, string> = {
  Summary: 'text-success',
  Regression: 'text-accent',
  Sprint: 'text-warning',
  Custom: 'text-muted-foreground',
};

const COLORS = {
  passed: 'hsl(152, 60%, 42%)',
  failed: 'hsl(350, 70%, 55%)',
  skipped: 'hsl(38, 90%, 55%)',
  untested: 'hsl(215, 15%, 65%)',
};

const defectStatusStyles: Record<string, string> = {
  Open: 'text-destructive',
  'In Progress': 'text-warning',
  Fixed: 'text-success',
  Closed: 'text-muted-foreground',
};

const defectSeverityStyles: Record<string, string> = {
  Critical: 'text-destructive',
  Major: 'text-warning',
  Minor: 'text-accent',
};

/** Derive a single case's overall status from its step statuses. */
function computeCaseStatus(steps: Array<{ status?: string }>): 'passed' | 'failed' | 'skipped' | 'untested' {
  if (!steps || steps.length === 0) return 'untested';
  if (steps.some((s) => s.status === 'failed'))  return 'failed';
  if (steps.every((s) => s.status === 'passed'))  return 'passed';
  if (steps.some((s) => s.status === 'skipped') &&
      !steps.some((s) => !s.status || s.status === 'untested')) return 'skipped';
  return 'untested';
}

const ReportDetail = () => {
  const { projectId, reportId } = useParams<{ projectId: string; reportId: string }>();
  const navigate = useNavigate();

  // Live data
  const { data: project } = useProject(projectId!);
  const { data: allRuns  = [] } = useTestRuns(projectId!);
  const { data: defects  = [] } = useDefects(projectId!);

  // ── Suite Analysis — real data pipeline ─────────────────────────────────────
  const { data: suites = [] } = useSuites(projectId!);

  const casesQueries = useQueries({
    queries: suites.map((suite) => ({
      queryKey: keys.cases.all(projectId!, suite.id),
      queryFn:  () => testCasesApi.list(projectId!, suite.id),
      enabled:  !!projectId && suites.length > 0,
      select:   (res: { data: Array<{ id: string; suiteId: string }> }) => res.data,
    })),
  });

  const casePairs = suites.flatMap((suite, i) =>
    (casesQueries[i]?.data ?? []).map((c) => ({ suiteId: suite.id, caseId: c.id }))
  );

  const stepsQueries = useQueries({
    queries: casePairs.map(({ suiteId, caseId }) => ({
      queryKey: keys.steps.all(projectId!, suiteId, caseId),
      queryFn:  () => testStepsApi.list(projectId!, suiteId, caseId),
      enabled:  !!projectId && casePairs.length > 0,
    })),
  });

  const updateDefect = useUpdateDefect();
  const deleteDefect = useDeleteDefect();

  // Stable display-ID maps
  const defectDisplayIdMap = useMemo(() => {
    const sorted = [...defects].sort((a, b) => (a.createdAt ?? '').localeCompare(b.createdAt ?? ''));
    const map: Record<string, string> = {};
    sorted.forEach((d, i) => { map[d.id] = d.displayId || listDisplayId('DEF', i); });
    return map;
  }, [defects]);

  const runDisplayIdMap = useMemo(() => {
    const sorted = [...allRuns].filter(Boolean).sort((a, b) => (a.createdAt || '').localeCompare(b.createdAt || ''));
    const map: Record<string, string> = {};
    sorted.forEach((r, i) => { map[r.id] = r.displayId || listDisplayId('TR', i); });
    return map;
  }, [allRuns]);

  // Load report from API
  const { data: report, isLoading: reportLoading } = useReport(projectId!, reportId!);

  const [deleteTarget, setDeleteTarget] = useState<{ id: string; displayId: string } | null>(null);

  // View modal
  const [viewOpen, setViewOpen] = useState(false);
  const [viewTarget, setViewTarget] = useState<Defect | null>(null);
  const [viewAttachments, setViewAttachments] = useState<any[]>([]);

  // Edit modal
  const [editOpen, setEditOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<Defect | null>(null);
  const [editAttachments, setEditAttachments] = useState<any[]>([]);

  // Tracks in-flight inline severity/status update
  const [updatingId, setUpdatingId] = useState<string | null>(null);

  // Suite breakdown — must be before early returns to satisfy Rules of Hooks
  const suiteData = useMemo(() => {
    if (!suites.length) return [];
    let pairIdx = 0;
    return suites
      .map((suite, i) => {
        const cases = casesQueries[i]?.data ?? [];
        let passed = 0, failed = 0, skipped = 0, untested = 0;
        cases.forEach(() => {
          const steps = (stepsQueries[pairIdx]?.data as Array<{ status?: string }> | undefined) ?? [];
          pairIdx++;
          const st = computeCaseStatus(steps);
          if (st === 'passed')       passed++;
          else if (st === 'failed')  failed++;
          else if (st === 'skipped') skipped++;
          else                       untested++;
        });
        return { suite: suite.name, passed, failed, skipped, untested };
      })
      .filter((s) => s.passed + s.failed + s.skipped + s.untested > 0);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [suites, casesQueries, stepsQueries]);

  if (reportLoading) {
    return (
      <div className="h-full flex items-center justify-center text-muted-foreground">
        <p>Loading report…</p>
      </div>
    );
  }

  if (!report) {
    return (
      <div className="h-full flex items-center justify-center text-muted-foreground">
        <p>Report not found.</p>
      </div>
    );
  }

  // Linked runs
  const linkedRunIds: string[] = report.selectedRuns ?? [];
  const linkedRuns = linkedRunIds.length > 0
    ? allRuns.filter((r) => linkedRunIds.includes(r.id))
    : allRuns;

  const totalPassed   = linkedRuns.reduce((s, r) => s + r.passed,   0);
  const totalFailed   = linkedRuns.reduce((s, r) => s + r.failed,   0);
  const totalSkipped  = linkedRuns.reduce((s, r) => s + r.skipped,  0);
  const totalUntested = linkedRuns.reduce((s, r) => s + r.untested, 0);
  const totalExecuted = totalPassed + totalFailed + totalSkipped;
  const totalCases    = totalPassed + totalFailed + totalSkipped + totalUntested;
  const passRate      = totalExecuted > 0 ? ((totalPassed / totalExecuted) * 100).toFixed(1) : '0.0';
  const executionProgress = totalCases > 0 ? (((totalCases - totalUntested) / totalCases) * 100) : 0;
  const remainingScope    = totalCases > 0 ? ((totalUntested / totalCases) * 100) : 0;
  const totalDefects      = defects.length;

  const pieData = [
    { name: 'Passed',   value: totalPassed,   color: COLORS.passed   },
    { name: 'Failed',   value: totalFailed,   color: COLORS.failed   },
    { name: 'Skipped',  value: totalSkipped,  color: COLORS.skipped  },
    { name: 'Untested', value: totalUntested, color: COLORS.untested },
  ].filter((d) => d.value > 0);

  const buildDefectUpdateBody = (d: Defect, overrides: Partial<Defect>): Partial<Defect> => ({
    title:       d.title,
    description: d.description,
    severity:    d.severity,
    status:      d.status,
    source:      d.source ?? '',
    link:        d.link ?? '',
    ...(d.createdAt ? { createdAt: d.createdAt } : {}),
    ...overrides,
  });

  const handleSeverityChange = (defectId: string, newSeverity: string) => {
    const d = defects.find((x) => x.id === defectId);
    if (!d || updatingId) return;
    setUpdatingId(defectId);
    updateDefect.mutate(
      { projectId: projectId!, id: defectId, body: buildDefectUpdateBody(d, { severity: newSeverity as Defect['severity'] }) },
      {
        onSuccess: () => toast.success(`Severity updated to ${newSeverity}`),
        onError:   (e) => toast.error(e.message),
        onSettled: () => setUpdatingId(null),
      }
    );
  };

  const handleStatusChange = (defectId: string, newStatus: string) => {
    const d = defects.find((x) => x.id === defectId);
    if (!d || updatingId) return;
    setUpdatingId(defectId);
    updateDefect.mutate(
      { projectId: projectId!, id: defectId, body: buildDefectUpdateBody(d, { status: newStatus as Defect['status'] }) },
      {
        onSuccess: () => toast.success(`Status updated to ${newStatus}`),
        onError:   (e) => toast.error(e.message),
        onSettled: () => setUpdatingId(null),
      }
    );
  };

  const handleDeleteDefect = () => {
    if (!deleteTarget) return;
    deleteDefect.mutate(
      { projectId: projectId!, id: deleteTarget.id },
      {
        onSuccess: () => {
          toast.success('Defect deleted');
          setDeleteTarget(null);
        },
        onError: (e) => toast.error(e.message),
      }
    );
  };

  const openView = (d: Defect) => {
    setViewTarget(d);
    setViewAttachments([]);
    attachmentsApi.listByDefect(projectId!, d.id)
      .then(atts => setViewAttachments((atts || []).map((a: any) => ({ id: a.id, name: a.fileName, size: a.fileSize, type: a.fileType ?? '' }))))
      .catch(() => setViewAttachments([]));
    setViewOpen(true);
  };

  const openEdit = (d: Defect) => {
    setEditTarget({ ...d });
    setEditAttachments([]);
    attachmentsApi.listByDefect(projectId!, d.id)
      .then(atts => setEditAttachments((atts || []).map((a: any) => ({ id: a.id, name: a.fileName, size: a.fileSize, type: a.fileType ?? '' }))))
      .catch(() => setEditAttachments([]));
    setEditOpen(true);
  };

  const handleEditDefectSave = async (updatedDefect: Defect, newFiles: File[], removedAttachmentIds: string[]) => {
    if (!projectId) return;
    try {
      await new Promise<void>((resolve, reject) => {
        updateDefect.mutate(
          { projectId: projectId!, id: updatedDefect.id, body: buildDefectUpdateBody(updatedDefect, {}) },
          { onSuccess: () => resolve(), onError: (e) => reject(e) }
        );
      });
      for (const file of newFiles) {
        try { await attachmentsApi.upload(projectId!, file, undefined, updatedDefect.id, 'DEFECT'); }
        catch { toast.warning(`Failed to upload ${file.name}`); }
      }
      for (const attachmentId of removedAttachmentIds) {
        try { await attachmentsApi.delete(projectId!, attachmentId); }
        catch { toast.warning('Failed to delete attachment'); }
      }
      toast.success('Defect updated');
      setEditOpen(false);
      setEditTarget(null);
      setEditAttachments([]);
    } catch (error: any) {
      toast.error(error?.message || 'Failed to update defect');
    }
  };

  const sortedDefects = [...defects].sort((a, b) => (b.createdAt ?? '').localeCompare(a.createdAt ?? ''));

  return (
    <div className="h-full flex flex-col">
      {/* Header */}
      <div className="px-6 pt-5 pb-4 bg-card border-b border-border/40 print:hidden">
        <div className="mb-2">
          <Breadcrumbs segments={[
            { label: 'Projects', href: '/projects' },
            { label: project?.name || 'Project', href: `/projects/${projectId}/repository` },
            { label: 'Reports', href: `/projects/${projectId}/reports` },
            { label: report.name },
          ]} />
        </div>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Button variant="ghost" size="icon" className="h-8 w-8" onClick={() => navigate(`/projects/${projectId}/reports`)}>
              <ArrowLeft className="h-4 w-4" strokeWidth={1.5} />
            </Button>
            <h1 className="text-xl font-bold text-foreground tracking-[-0.02em]">{report.name}</h1>
            <span className={`${typeBadge[report.type] || 'text-muted-foreground'} text-[10px] px-2.5 py-0.5 font-mono uppercase tracking-widest inline-flex items-center gap-1`}>
              {report.type}
            </span>
          </div>
          <Button size="sm" variant="outline" className="gap-1.5" onClick={() => window.print()}>
            <Download className="h-3.5 w-3.5" strokeWidth={1.5} /> Export PDF
          </Button>
        </div>
      </div>

      {/* Content — centered canvas */}
      <div className="flex-1 overflow-auto p-6 bg-background flex justify-center print:block print:overflow-visible print:p-4">
        <div className="w-full max-w-[1200px] space-y-6 pb-20">

          {/* Executive Summary */}
          {report.sectionExecutiveSummary && (
            <div className="bg-card rounded-xl border border-border/40 shadow-sm overflow-hidden">
              <div className="px-5 py-3.5 border-b border-border/40">
                <h2 className="text-sm font-semibold text-foreground uppercase tracking-wider">Executive Summary</h2>
                <p className="text-xs text-muted-foreground mt-1">
                  Source Runs:{' '}
                  {linkedRuns.map((run, i) => (
                    <span key={run.id}>
                      <a
                        href={`/projects/${projectId}/test-runs/${run.id}`}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-accent hover:underline"
                      >
                        {run.name}
                      </a>
                      {i < linkedRuns.length - 1 && ', '}
                    </span>
                  ))}
                </p>
              </div>
              <div className="p-5">
                <div className="flex flex-col md:grid md:grid-cols-2 gap-5 items-stretch">
                  {/* Donut Chart + Legend — 30% left, centered vertically */}
                  <div className="flex flex-col items-center justify-center gap-2">
                    <div className="w-72 h-72">
                      <ResponsiveContainer width="100%" height="100%">
                        <PieChart>
                          <Pie
                            data={pieData}
                            cx="50%"
                            cy="50%"
                            innerRadius={62}
                            outerRadius={108}
                            paddingAngle={3}
                            dataKey="value"
                            strokeWidth={0}
                          >
                            {pieData.map((entry, idx) => (
                              <Cell key={idx} fill={entry.color} />
                            ))}
                          </Pie>
                          <Tooltip
                            contentStyle={{ background: 'hsl(var(--card))', border: '1px solid hsl(var(--border))', borderRadius: '8px', fontSize: '12px' }}
                            itemStyle={{ color: 'hsl(var(--foreground))' }}
                          />
                        </PieChart>
                      </ResponsiveContainer>
                    </div>
                    {/* Legend — 2 cols × 2 rows, compact */}
                    <div className="grid grid-cols-2 gap-x-4 gap-y-1.5">
                      {pieData.map((d) => {
                        const pct = totalCases > 0 ? ((d.value / totalCases) * 100).toFixed(0) : '0';
                        return (
                          <div key={d.name} className="flex items-center gap-1.5">
                            <div className="h-2 w-2 rounded-full shrink-0" style={{ background: d.color }} />
                            <span className="text-[11px] text-muted-foreground whitespace-nowrap">
                              {d.name}: <span className="font-semibold text-foreground">{d.value}</span>{' '}
                              <span className="opacity-70">({pct}%)</span>
                            </span>
                          </div>
                        );
                      })}
                    </div>
                  </div>
                  {/* KPI Cards — 70% right, 2 cols × 3 rows, fills column height */}
                  <div className="h-full grid grid-cols-2 grid-rows-3 gap-2.5">
                    <div className="bg-background rounded-lg border border-border/40 p-3 flex flex-col items-center justify-center">
                      <p className="text-2xl font-bold text-foreground">{totalCases}</p>
                      <p className="text-[10px] uppercase tracking-widest text-muted-foreground font-semibold mt-1">Total Cases</p>
                    </div>
                    <div className="bg-background rounded-lg border border-border/40 p-3 flex flex-col items-center justify-center">
                      <p className="text-2xl font-bold text-success">{passRate}%</p>
                      <p className="text-[10px] uppercase tracking-widest text-muted-foreground font-semibold mt-1">Pass Rate</p>
                      <p className="text-[9px] text-muted-foreground mt-0.5">(of executed)</p>
                    </div>
                    <div className="bg-background rounded-lg border border-border/40 p-3 flex flex-col items-center justify-center">
                      <p className="text-2xl font-bold text-destructive">{totalDefects}</p>
                      <p className="text-[10px] uppercase tracking-widest text-muted-foreground font-semibold mt-1">Total Defects</p>
                    </div>
                    <div className="bg-background rounded-lg border border-border/40 p-3 flex flex-col items-center justify-center">
                      <p className="text-2xl font-bold text-[hsl(215,60%,55%)]">{executionProgress.toFixed(1)}%</p>
                      <p className="text-[10px] uppercase tracking-widest text-muted-foreground font-semibold mt-1">Execution Progress</p>
                      <div className="mt-2 h-1 w-full rounded-full bg-border/60 overflow-hidden">
                        <div className="h-full rounded-full bg-[hsl(215,60%,55%)] transition-all duration-500" style={{ width: `${executionProgress}%` }} />
                      </div>
                    </div>
                    <div className="bg-background rounded-lg border border-border/40 p-3 flex flex-col items-center justify-center">
                      <p className="text-2xl font-bold" style={{ color: COLORS.untested }}>{totalUntested}</p>
                      <p className="text-[10px] uppercase tracking-widest text-muted-foreground font-semibold mt-1">Untested</p>
                    </div>
                    <div className="bg-background rounded-lg border border-border/40 p-3 flex flex-col items-center justify-center">
                      <p className="text-2xl font-bold text-muted-foreground">{remainingScope.toFixed(1)}%</p>
                      <p className="text-[10px] uppercase tracking-widest text-muted-foreground font-semibold mt-1">Remaining Scope</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Environment Info */}
          {report.sectionEnvironmentInfo && linkedRuns.length > 0 && (
            <div className="bg-card rounded-xl border border-border/40 shadow-sm overflow-hidden">
              <div className="px-5 py-3.5 border-b border-border/40">
                <h2 className="text-sm font-semibold text-foreground uppercase tracking-wider">Environment Details</h2>
              </div>
              <div className="px-5 py-3">
                <div className="grid grid-cols-3 gap-3">
                  <div className="bg-background rounded-lg border border-border/40 px-3 py-2.5">
                    <p className="text-[10px] uppercase tracking-widest text-muted-foreground font-semibold mb-1">Environment(s)</p>
                    <div className="flex flex-wrap gap-1.5">
                      {[...new Set(linkedRuns.map((r) => r.environment))].map((env) => (
                        <span key={env} className="text-xs font-mono bg-secondary px-2 py-0.5 rounded text-foreground">{env}</span>
                      ))}
                    </div>
                  </div>
                  <div className="bg-background rounded-lg border border-border/40 px-3 py-2.5">
                    <p className="text-[10px] uppercase tracking-widest text-muted-foreground font-semibold mb-1">Build Version(s)</p>
                    <div className="flex flex-wrap gap-1.5">
                      {[...new Set(linkedRuns.map((r) => r.buildVersion).filter(Boolean))].map((v) => (
                        <span key={v} className="text-xs font-mono bg-secondary px-2 py-0.5 rounded text-foreground">{v}</span>
                      ))}
                    </div>
                  </div>
                  <div className="bg-background rounded-lg border border-border/40 px-3 py-2.5">
                    <p className="text-[10px] uppercase tracking-widest text-muted-foreground font-semibold mb-1">Created By</p>
                    <p className="text-[10px] uppercase tracking-widest text-muted-foreground font-semibold">{report.createdBy}</p>
                    <p className="text-[10px] text-muted-foreground font-mono mt-0.5">{formatDate(report.createdAt)}</p>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Suite Analysis */}
          {report.sectionSuiteAnalytics && (
            <div className="bg-card rounded-xl border border-border/40 shadow-sm overflow-hidden">
              <div className="px-5 py-3.5 border-b border-border/40">
                <h2 className="text-sm font-semibold text-foreground uppercase tracking-wider">Suite Analysis</h2>
              </div>
              <div className="p-5">
                <div className="h-64">
                  <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={suiteData} barCategoryGap="25%">
                      <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" vertical={false} />
                      <XAxis
                        dataKey="suite"
                        tick={{ fontSize: 11, fill: 'hsl(var(--muted-foreground))' }}
                        axisLine={{ stroke: 'hsl(var(--border))' }}
                        tickLine={false}
                      />
                      <YAxis
                        tick={{ fontSize: 11, fill: 'hsl(var(--muted-foreground))' }}
                        axisLine={false}
                        tickLine={false}
                      />
                      <Tooltip
                        contentStyle={{ background: 'hsl(var(--card))', border: '1px solid hsl(var(--border))', borderRadius: '8px', fontSize: '12px' }}
                        itemStyle={{ color: 'hsl(var(--foreground))' }}
                      />
                      <Legend
                        iconType="circle"
                        iconSize={8}
                        wrapperStyle={{ fontSize: '11px', color: 'hsl(var(--muted-foreground))' }}
                      />
                      <Bar dataKey="passed" name="Passed" fill={COLORS.passed} radius={[3, 3, 0, 0]} stackId="a" />
                      <Bar dataKey="failed" name="Failed" fill={COLORS.failed} radius={[0, 0, 0, 0]} stackId="a" />
                      <Bar dataKey="skipped" name="Skipped" fill={COLORS.skipped} radius={[0, 0, 0, 0]} stackId="a" />
                      <Bar dataKey="untested" name="Untested" fill={COLORS.untested} radius={[3, 3, 0, 0]} stackId="a" />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              </div>
            </div>
          )}

          {/* Defect Table */}
          {report.sectionDefectTable && (
            <div className="bg-card rounded-xl border border-border/40 shadow-sm overflow-hidden">
              <div className="px-5 py-3.5 border-b border-border/40">
                <h2 className="text-sm font-semibold text-foreground uppercase tracking-wider">Defect Log</h2>
              </div>
              <table className="w-full text-sm">
                <thead>
                  <tr className="bg-slate-200 dark:bg-slate-700 sticky top-0 z-10">
                    <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">ID</th>
                    <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Title</th>
                    <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Severity</th>
                    <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Status</th>
                    <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Source</th>
                    <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Created</th>
                    <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider w-px whitespace-nowrap">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {sortedDefects.length === 0 ? (
                    <tr>
                      <td colSpan={7} className="text-center py-12 text-muted-foreground text-sm">No defects linked to this report</td>
                    </tr>
                  ) : (
                    sortedDefects.map((d) => (
                      <tr
                        key={d.id}
                        className="hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-colors border-b border-slate-100 dark:border-slate-700/50 bg-card cursor-pointer"
                        onClick={() => openView(d)}
                      >
                        <td className="px-5 py-3.5 font-mono text-[10px] text-accent tracking-wider" title={d.id}>{defectDisplayIdMap[d.id] ?? '-'}</td>
                        <td className="px-5 py-3.5 font-medium text-foreground">{d.title}</td>
                        <td className="px-5 py-3.5" onClick={(e) => e.stopPropagation()}>
                          {updatingId === d.id ? (
                            <span className="inline-flex items-center gap-1 h-7 px-2.5 text-[10px] font-mono uppercase tracking-widest text-muted-foreground">
                              <Loader2 className="h-3 w-3 animate-spin" /> {d.severity}
                            </span>
                          ) : (
                            <Select value={d.severity} onValueChange={(val) => handleSeverityChange(d.id, val)} disabled={!!updatingId}>
                              <SelectTrigger className={`h-7 w-auto text-[10px] font-mono uppercase tracking-widest rounded-md px-2.5 ${defectSeverityStyles[d.severity] || ''}`}>
                                <SelectValue />
                              </SelectTrigger>
                              <SelectContent>
                                <SelectItem value="Critical">Critical</SelectItem>
                                <SelectItem value="Major">Major</SelectItem>
                                <SelectItem value="Minor">Minor</SelectItem>
                              </SelectContent>
                            </Select>
                          )}
                        </td>
                        <td className="px-5 py-3.5" onClick={(e) => e.stopPropagation()}>
                          {updatingId === d.id ? (
                            <span className="inline-flex items-center gap-1 h-7 px-2.5 text-[10px] font-mono uppercase tracking-widest text-muted-foreground">
                              <Loader2 className="h-3 w-3 animate-spin" /> {d.status}
                            </span>
                          ) : (
                            <Select value={d.status} onValueChange={(val) => handleStatusChange(d.id, val)} disabled={!!updatingId}>
                              <SelectTrigger className={`h-7 w-auto text-[10px] font-mono uppercase tracking-widest rounded-md px-2.5 ${defectStatusStyles[d.status] || ''}`}>
                                <SelectValue />
                              </SelectTrigger>
                              <SelectContent>
                                <SelectItem value="Open">Open</SelectItem>
                                <SelectItem value="In Progress">In Progress</SelectItem>
                                <SelectItem value="Fixed">Fixed</SelectItem>
                                <SelectItem value="Closed">Closed</SelectItem>
                              </SelectContent>
                            </Select>
                          )}
                        </td>
                        <td className="px-5 py-3.5 font-mono text-[10px] text-muted-foreground tracking-wider">
                          {(() => {
                            const parts: string[] = [];
                            if (d.testRunId) {
                              const trLabel = runDisplayIdMap[d.testRunId];
                              if (trLabel) parts.push(trLabel);
                            }
                            if (d.source && !isUuid(d.source)) parts.push(d.source);
                            return parts.length > 0 ? parts.join(' · ') : '—';
                          })()}
                        </td>
                        <td className="px-5 py-3.5 text-muted-foreground font-mono text-[10px] tracking-wider">{formatDate(d.createdAt)}</td>
                        <td className="px-5 py-3.5 w-px whitespace-nowrap" onClick={(e) => e.stopPropagation()}>
                          <div className="flex items-center gap-1">
                            <button
                              onClick={() => openView(d)}
                              className="p-1.5 rounded hover:bg-muted text-muted-foreground hover:text-foreground transition-colors"
                            >
                              <Eye className="h-3.5 w-3.5" strokeWidth={1.5} />
                            </button>
                            <button
                              onClick={() => openEdit(d)}
                              className="p-1.5 rounded hover:bg-muted text-muted-foreground hover:text-foreground transition-colors"
                            >
                              <Pencil className="h-3.5 w-3.5" strokeWidth={1.5} />
                            </button>
                            <button
                              onClick={() => setDeleteTarget({ id: d.id, displayId: defectDisplayIdMap[d.id] ?? d.id })}
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
                    ))
                  )}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* View Defect Modal */}
      <ViewDefectModal
        open={viewOpen}
        onOpenChange={setViewOpen}
        defect={viewTarget}
        displayId={viewTarget ? defectDisplayIdMap[viewTarget.id] : undefined}
        existingAttachments={viewAttachments}
        formatDate={formatDate}
        projectId={projectId}
      />

      {/* Edit Defect Modal */}
      <EditDefectModal
        open={editOpen}
        onOpenChange={setEditOpen}
        defect={editTarget}
        displayId={editTarget ? defectDisplayIdMap[editTarget.id] : undefined}
        existingAttachments={editAttachments}
        onSave={handleEditDefectSave}
        projectId={projectId}
      />

      {/* Delete Confirmation */}
      <Dialog open={deleteTarget !== null} onOpenChange={(open) => { if (!open) setDeleteTarget(null); }}>
        <DialogContent className="sm:max-w-sm bg-card">
          <DialogHeader>
            <DialogTitle className="text-foreground flex items-center gap-3">
              <AlertTriangle className="h-6 w-6 text-destructive shrink-0" strokeWidth={1.5} />
              Remove Defect
            </DialogTitle>
            <DialogDescription className="text-sm text-foreground mt-3">
              Are you sure you want to delete <span className="font-bold font-mono text-purple-600 dark:text-purple-400">{deleteTarget?.displayId}</span>? This action cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter className="mt-6">
            <Button variant="outline" onClick={() => setDeleteTarget(null)}>Cancel</Button>
            <Button variant="destructive" onClick={handleDeleteDefect}>Remove</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

    </div>
  );
};

export default ReportDetail;
