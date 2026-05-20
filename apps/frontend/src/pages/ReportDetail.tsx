import { useParams, useNavigate } from 'react-router-dom';
import Breadcrumbs from '@/components/Breadcrumbs';
import { Button } from '@/components/ui/button';
import { ArrowLeft, Download, ExternalLink } from 'lucide-react';
import { useProject, useTestRuns, useDefects, useSuites, useReport, useTestRunCasesForRuns, useRepository, useTestRunsForIds } from '@/hooks/useApi';
import type { Defect } from '@/lib/api';
import type { ReportSnapshotData, SnapshotDefect, CaseToSuiteMap } from '@/lib/reportSnapshot';
import { computeSnapshotData, resolveReportDefects } from '@/lib/reportSnapshot';
import { formatDate } from '@/lib/utils';
import { useMemo } from 'react';
import { deduplicateRunCases, groupBySuite } from '@/lib/reportAggregation';
import type { RunCaseWithMeta } from '@/lib/reportAggregation';
import {
  PieChart, Pie, Cell, ResponsiveContainer, Tooltip,
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Legend,
} from 'recharts';

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

const ReportDetail = () => {
  const { projectId, reportId } = useParams<{ projectId: string; reportId: string }>();
  const navigate = useNavigate();

  // Live data
  const { data: project }        = useProject(projectId!);
  const { data: allRuns  = [] }  = useTestRuns(projectId!);
  const { data: defects  = [] }  = useDefects(projectId!);
  const { data: suites   = [] }  = useSuites(projectId!);
  const { data: report, isLoading: reportLoading } = useReport(projectId!, reportId!);
  const { data: repositoryData }                   = useRepository(projectId!);

  const caseToSuiteMap = useMemo((): CaseToSuiteMap => {
    const map = new Map<string, string>();
    (repositoryData?.suites ?? []).forEach(s => s.cases.forEach(c => map.set(c.id, s.id)));
    return map;
  }, [repositoryData]);

  const parsedSnapshot = useMemo((): ReportSnapshotData | null => {
    if (!report?.snapshotData) return null;
    try { return JSON.parse(report.snapshotData) as ReportSnapshotData; }
    catch { return null; }
  }, [report?.snapshotData]);

  // Resolve run IDs before early returns so hook call count is stable.
  // Skip live fetch only when snapshot is fully populated (has suite data too).
  // When suiteData is empty we still need TestRunCase records for the suite chart.
  const resolvedRunIds = useMemo(() => {
    if (parsedSnapshot?.suiteData?.length) return [];   // complete snapshot — no fetch needed
    if (!report) return [];
    const ids = report.selectedRuns ?? [];
    return ids.length > 0 ? ids : allRuns.map(r => r.id);
  }, [parsedSnapshot, report, allRuns]);

  const runCasesQueries = useTestRunCasesForRuns(projectId!, resolvedRunIds);

  // Stable flattened data — React Query's dataUpdatedAt only changes when data changes,
  // so this memo does not re-run on every render tick.
  const rawRunCases = useMemo(
    () => runCasesQueries.flatMap(q => q.data ?? []),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [runCasesQueries.map(q => q.dataUpdatedAt).join(), allRuns],
  );

  // Parse each run's flat step-status map (caseId::stepNumber → STATUS).
  // run.stepStatuses is the ground truth — it is saved on every step click and is
  // more reliable than TestRunCase.status, which can lag if the SnapshotProcessor
  // hadn't finished creating TestRunCase records when the user first clicked steps.
  const runStepStatusMap = useMemo(() => {
    const map = new Map<string, Record<string, string>>();
    allRuns.forEach(run => {
      if (!run.stepStatuses) return;
      try {
        const parsed: Record<string, string> =
          typeof run.stepStatuses === 'string'
            ? JSON.parse(run.stepStatuses as unknown as string)
            : (run.stepStatuses as unknown as Record<string, string>);
        if (Object.keys(parsed).length > 0) map.set(run.id, parsed);
      } catch { /* ignore malformed JSON */ }
    });
    return map;
  }, [allRuns]);

  const deduped = useMemo((): RunCaseWithMeta[] => {
    const runCreatedAtMap = new Map(allRuns.map(r => [r.id, r.createdAt ?? '']));
    const all: RunCaseWithMeta[] = rawRunCases.map(rc => {
      // Derive status from the run's flat step map when available — this is the
      // same source the run-page progress bar uses, so Suite Analysis now agrees.
      const stepMap = runStepStatusMap.get(rc.testRunId);
      let status = rc.status;
      if (stepMap) {
        const prefix = `${rc.testCaseId}::`;
        const caseSteps = Object.entries(stepMap)
          .filter(([k]) => k.startsWith(prefix))
          .map(([, v]) => v.toLowerCase());
        if (caseSteps.length > 0) {
          if (caseSteps.some(s => s === 'failed'))
            status = 'FAILED';
          else if (caseSteps.every(s => s === 'passed'))
            status = 'PASSED';
          else if (caseSteps.some(s => s === 'skipped') && !caseSteps.some(s => s === 'untested'))
            status = 'SKIPPED';
          else
            status = 'UNTESTED';
        }
      }
      return {
        testCaseId:   rc.testCaseId,
        suiteId:      rc.suiteId ?? '',
        testRunId:    rc.testRunId,
        status,
        runCreatedAt: runCreatedAtMap.get(rc.testRunId) ?? '',
      };
    });
    return deduplicateRunCases(all);
  }, [rawRunCases, allRuns, runStepStatusMap]);

  // Resolve linked run IDs — must happen before hooks below.
  const linkedRunIds = report?.selectedRuns ?? [];

  // Fetch each selected run individually via the detail endpoint so we get the full
  // entity including caseIds (the list endpoint may not return array fields).
  const linkedRunQueries = useTestRunsForIds(projectId!, linkedRunIds);
  const linkedRuns = useMemo(() => {
    const fetched = linkedRunQueries.map(q => q.data).filter(Boolean) as import('@/lib/api').TestRun[];
    if (fetched.length > 0) return fetched;
    // Fallback to allRuns while individual fetches are loading.
    return linkedRunIds.length > 0 ? allRuns.filter(r => linkedRunIds.includes(r.id)) : allRuns;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [linkedRunQueries.map(q => q.dataUpdatedAt).join(), allRuns, linkedRunIds.join(',')]);

  const suiteData = useMemo(() => {
    if (parsedSnapshot?.suiteData?.length) return parsedSnapshot.suiteData;

    const suiteNameMap = new Map(suites.map(s => [s.id, s.name]));
    const suiteOrderMap = new Map(
      [...suites]
        .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
        .map((s, i) => [s.name, i]),
    );
    const sort = (data: ReturnType<typeof groupBySuite>) =>
      data.sort((a, b) => (suiteOrderMap.get(a.suite) ?? Infinity) - (suiteOrderMap.get(b.suite) ?? Infinity));

    // Enrich deduped records: use caseToSuiteMap as fallback when suiteId is empty.
    const enriched: RunCaseWithMeta[] = deduped.map(rc => ({
      ...rc,
      suiteId: rc.suiteId || caseToSuiteMap.get(rc.testCaseId) || '',
    }));

    // Add UNTESTED entries for cases in run.caseIds that have no TestRunCase record.
    const knownCaseIds = new Set(enriched.map(rc => rc.testCaseId));
    const runCreatedAtMap = new Map(linkedRuns.map(r => [r.id, r.createdAt ?? '']));
    const extraCases: RunCaseWithMeta[] = linkedRuns.flatMap(run => {
      const stepMap = runStepStatusMap.get(run.id) ?? {};
      return (run.caseIds ?? []).flatMap(caseId => {
        if (knownCaseIds.has(caseId)) return [];
        const suiteId = caseToSuiteMap.get(caseId) ?? '';
        if (!suiteId) return [];
        const caseSteps = Object.entries(stepMap)
          .filter(([k]) => k.startsWith(`${caseId}::`))
          .map(([, v]) => v.toLowerCase());
        const status = caseSteps.length === 0 ? 'UNTESTED'
          : caseSteps.some(s => s === 'failed') ? 'FAILED'
          : caseSteps.every(s => s === 'passed') ? 'PASSED'
          : caseSteps.some(s => s === 'skipped') && !caseSteps.some(s => s === 'untested') ? 'SKIPPED'
          : 'UNTESTED';
        return [{ testCaseId: caseId, suiteId, testRunId: run.id, status, runCreatedAt: runCreatedAtMap.get(run.id) ?? '' }];
      });
    });

    const allCases = [...enriched, ...deduplicateRunCases(extraCases)];
    if (allCases.length > 0) return sort(groupBySuite(allCases, suiteNameMap));

    // Final fallback: derive from run.caseIds + run.stepStatuses + caseToSuiteMap.
    const liveSnapshot = computeSnapshotData([], linkedRuns, suites, [], caseToSuiteMap);
    return liveSnapshot.suiteData;
  }, [parsedSnapshot, deduped, linkedRuns, suites, caseToSuiteMap, runStepStatusMap]);

  // Must be called before any early returns — Rules of Hooks
  const defectRows = useMemo(
    () => resolveReportDefects(parsedSnapshot?.defects, defects),
    [parsedSnapshot?.defects, defects],
  );

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

  // KPI totals: snapshot → per-case computation → run-level aggregates (fallback when
  // TestRunCase records are missing, e.g. Cyoda schema not yet re-imported after refactor).
  const totalPassed   = parsedSnapshot?.totalPassed   ?? (deduped.length > 0
    ? deduped.filter(rc => rc.status === 'PASSED').length
    : linkedRuns.reduce((s, r) => s + (r.passed  ?? 0), 0));
  const totalFailed   = parsedSnapshot?.totalFailed   ?? (deduped.length > 0
    ? deduped.filter(rc => rc.status === 'FAILED').length
    : linkedRuns.reduce((s, r) => s + (r.failed  ?? 0), 0));
  const totalSkipped  = parsedSnapshot?.totalSkipped  ?? (deduped.length > 0
    ? deduped.filter(rc => rc.status === 'SKIPPED').length
    : linkedRuns.reduce((s, r) => s + (r.skipped ?? 0), 0));
  const totalUntested = parsedSnapshot?.totalUntested ?? (deduped.length > 0
    ? deduped.filter(rc => rc.status === 'UNTESTED').length
    : linkedRuns.reduce((s, r) => s + (r.untested ?? 0), 0));
  const totalExecuted = totalPassed + totalFailed + totalSkipped;
  const totalCases    = totalPassed + totalFailed + totalSkipped + totalUntested;
  const passRate      = totalExecuted > 0 ? ((totalPassed / totalExecuted) * 100).toFixed(1) : '0.0';
  const executionProgress = totalCases > 0 ? (((totalCases - totalUntested) / totalCases) * 100) : 0;
  const remainingScope    = totalCases > 0 ? ((totalUntested / totalCases) * 100) : 0;
  const totalDefects      = defectRows.length;

  const pieData = [
    { name: 'Passed',   value: totalPassed,   color: COLORS.passed   },
    { name: 'Failed',   value: totalFailed,   color: COLORS.failed   },
    { name: 'Skipped',  value: totalSkipped,  color: COLORS.skipped  },
    { name: 'Untested', value: totalUntested, color: COLORS.untested },
  ].filter((d) => d.value > 0);

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
      <div className="flex-1 overflow-auto p-6 bg-background print:block print:overflow-visible print:p-4">
        <div className="w-full max-w-[1200px] mx-auto space-y-6 pb-8">

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
                    {/* Legend — single row */}
                    <div className="flex flex-wrap gap-x-4 gap-y-1.5">
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
              <div className="px-5 py-2.5">
                <div className="grid grid-cols-3 gap-3">
                  <div className="bg-background rounded-lg border border-border/40 px-3 py-2">
                    <p className="text-[10px] uppercase tracking-widest text-muted-foreground font-semibold mb-1">Environment(s)</p>
                    <div className="flex flex-wrap gap-1.5">
                      {[...new Set(linkedRuns.map((r) => r.environment))].map((env) => (
                        <span key={env} className="text-xs font-mono bg-secondary px-2 py-0.5 rounded text-foreground">{env}</span>
                      ))}
                    </div>
                  </div>
                  <div className="bg-background rounded-lg border border-border/40 px-3 py-2">
                    <p className="text-[10px] uppercase tracking-widest text-muted-foreground font-semibold mb-1">Build Version(s)</p>
                    <div className="flex flex-wrap gap-1.5">
                      {[...new Set(linkedRuns.map((r) => r.buildVersion).filter(Boolean))].map((v) => (
                        <span key={v} className="text-xs font-mono bg-secondary px-2 py-0.5 rounded text-foreground">{v}</span>
                      ))}
                    </div>
                  </div>
                  <div className="bg-background rounded-lg border border-border/40 px-3 py-2">
                    <p className="text-[10px] uppercase tracking-widest text-muted-foreground font-semibold mb-1">Created By</p>
                    <div className="flex flex-wrap gap-1.5">
                      <span className="text-xs font-mono bg-secondary px-2 py-0.5 rounded text-foreground">{report.createdBy}</span>
                      <span className="text-xs font-mono bg-secondary px-2 py-0.5 rounded text-foreground">{formatDate(report.createdAt)}</span>
                    </div>
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
                      <Bar dataKey="passed" name="Passed" fill={COLORS.passed} radius={[0, 0, 0, 0]} stackId="a" />
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
                    <th className="px-5 py-3 w-px"></th>
                  </tr>
                </thead>
                <tbody>
                  {(() => {
                    const rows: Array<Defect | SnapshotDefect> = defectRows;

                    if (rows.length === 0) {
                      return (
                        <tr>
                          <td colSpan={7} className="text-center py-12 text-muted-foreground text-sm">
                            No defects linked to this report
                          </td>
                        </tr>
                      );
                    }

                    return rows.map((d, index) => (
                      <tr
                        key={d.id || `defect-${index}`}
                        className="hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-colors border-b border-slate-100 dark:border-slate-700/50 bg-card"
                      >
                        <td className="px-5 py-3.5 font-mono text-[10px] text-accent tracking-wider" title={String(d.id)}>
                          {d.displayId || '—'}
                        </td>
                        <td className="px-5 py-3.5 font-medium text-foreground">{d.title || '—'}</td>
                        <td className="px-5 py-3.5">
                          <span className={`text-[10px] font-mono uppercase tracking-widest ${defectSeverityStyles[d.severity] || ''}`}>
                            {d.severity || '—'}
                          </span>
                        </td>
                        <td className="px-5 py-3.5">
                          <span className={`text-[10px] font-mono uppercase tracking-widest ${defectStatusStyles[d.status] || ''}`}>
                            {d.status || '—'}
                          </span>
                        </td>
                        <td className="px-5 py-3.5 font-mono text-[10px] text-muted-foreground tracking-wider">
                          {d.source || '—'}
                        </td>
                        <td className="px-5 py-3.5 text-muted-foreground font-mono text-[10px] tracking-wider">
                          {d.createdAt ? formatDate(d.createdAt) : '—'}
                        </td>
                        <td className="px-5 py-3.5 w-px whitespace-nowrap">
                          {d.link && (
                            <a
                              href={d.link}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="p-1.5 rounded hover:bg-muted text-muted-foreground hover:text-foreground transition-colors inline-flex"
                            >
                              <ExternalLink className="h-3.5 w-3.5" strokeWidth={1.5} />
                            </a>
                          )}
                        </td>
                      </tr>
                    ));
                  })()}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

    </div>
  );
};

export default ReportDetail;
