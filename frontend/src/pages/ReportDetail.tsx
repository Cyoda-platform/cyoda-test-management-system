import { useParams, useNavigate } from 'react-router-dom';
import Breadcrumbs from '@/components/Breadcrumbs';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { ArrowLeft, Download, FileText, Table2, ExternalLink, Trash2, AlertTriangle, Eye, Pencil } from 'lucide-react';
import { useProject, useTestRuns, useDefects, useUpdateDefect, useDeleteDefect } from '@/hooks/useApi';
import type { Defect } from '@/lib/api';
import { listDisplayId, formatDate, isUuid } from '@/lib/utils';
import { useMemo } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@/components/ui/dialog';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { useState } from 'react';
import {
  PieChart, Pie, Cell, ResponsiveContainer, Tooltip,
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Legend,
} from 'recharts';

interface ReportSections {
  executiveSummary: boolean;
  suiteAnalytics: boolean;
  defectTable: boolean;
  environmentInfo: boolean;
}

interface ReportMeta {
  id: string;
  name: string;
  type: string;
  createdBy: string;
  date: string;
  summary?: string;
  sections: ReportSections;
  linkedRuns?: string[];    // hardcoded fallback format
  selectedRuns?: string[];  // saved by CreateReport
  description?: string;
  dateFrom?: string;
  dateTo?: string;
}

const reportData: Record<string, ReportMeta> = {
  'REP-01': {
    name: 'Weekly Regression - Week 12',
    type: 'Regression',
    createdBy: 'admin',
    date: '2026-03-20',
    summary: 'Full regression cycle covering 57 test cases across Authorization, Dashboard, and API Integration suites. 52 passed, 3 failed, 2 skipped.',
    sections: { executiveSummary: true, suiteAnalytics: true, defectTable: true, environmentInfo: true },
    linkedRuns: ['TR-01', 'TR-02'],
  },
  'REP-02': {
    name: 'Sprint 11 Summary',
    type: 'Sprint',
    createdBy: 'qa_lead',
    date: '2026-03-14',
    summary: 'Sprint 11 release validation. All critical paths verified against acceptance criteria.',
    sections: { executiveSummary: true, suiteAnalytics: false, defectTable: false, environmentInfo: true },
    linkedRuns: ['TR-02'],
  },
  'REP-03': {
    name: 'Security Audit Report',
    type: 'Custom',
    createdBy: 'admin',
    date: '2026-03-05',
    summary: 'SOC2 compliance audit tracking. Penetration tests and vulnerability scans completed.',
    sections: { executiveSummary: true, suiteAnalytics: true, defectTable: true, environmentInfo: false },
    linkedRuns: ['TR-03', 'TR-04'],
  },
};

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

const DEFECT_STATUSES = ['Open', 'In Progress', 'Fixed', 'Closed'] as const;
const DEFECT_SEVERITIES = ['Critical', 'Major', 'Minor'] as const;

const ReportDetail = () => {
  const { projectId, reportId } = useParams<{ projectId: string; reportId: string }>();
  const navigate = useNavigate();

  // Live data
  const { data: project } = useProject(projectId!);
  const { data: allRuns  = [] } = useTestRuns(projectId!);
  const { data: defects  = [] } = useDefects(projectId!);

  const updateDefect = useUpdateDefect();
  const deleteDefect = useDeleteDefect();

  // Build a stable display-ID map (DEF-01, DEF-02…) ordered by creation time
  const defectDisplayIdMap = useMemo(() => {
    const sorted = [...defects].sort((a, b) => a.createdAt.localeCompare(b.createdAt));
    const map: Record<string, string> = {};
    sorted.forEach((d, i) => { map[d.id] = listDisplayId('DEF', i); });
    return map;
  }, [defects]);

  // Load report config: try localStorage first, fall back to hardcoded reportData
  const report = useMemo<ReportMeta | null>(() => {
    const key = `reports-${projectId}`;
    try {
      const saved = localStorage.getItem(key);
      if (saved) {
        const parsed = JSON.parse(saved) as ReportMeta[];
        const found = parsed.find((r) => r.id === reportId);
        if (found) return found;
      }
    } catch {}
    // fallback to hardcoded data for the pre-seeded sample reports
    const fallback = (reportData as Record<string, Omit<ReportMeta, 'id'>>)[reportId ?? ''];
    return fallback ? ({ ...fallback, id: reportId! }) : null;
  }, [projectId, reportId]);

  const [downloadOpen, setDownloadOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null);
  const [viewTarget, setViewTarget] = useState<Defect | null>(null);
  const [viewOpen, setViewOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<Defect | null>(null);
  const [editOpen, setEditOpen] = useState(false);

  if (!report) {
    return (
      <div className="h-full flex items-center justify-center text-muted-foreground">
        <p>Report not found.</p>
      </div>
    );
  }

  // Linked runs — support both field names written by CreateReport vs hardcoded reportData
  const linkedRunIds: string[] = report.selectedRuns ?? report.linkedRuns ?? [];
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

  // Suite breakdown — placeholder; real data would require suite-level run stats from the API
  const suiteData = [
    { suite: 'Authorization',  passed: 18, failed: 1, skipped: 1, untested: 2 },
    { suite: 'Dashboard',      passed: 14, failed: 1, skipped: 0, untested: 3 },
    { suite: 'API Integration', passed: 20, failed: 1, skipped: 1, untested: 5 },
  ];

  const handleDownload = (format: string) => {
    const blob = new Blob([`${report.name} — exported as ${format}`], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${report.name.replace(/\s+/g, '_')}.${format}`;
    a.click();
    URL.revokeObjectURL(url);
    setDownloadOpen(false);
  };

  const handleStatusChange = (defectId: string, newStatus: string) => {
    updateDefect.mutate({ projectId: projectId!, id: defectId, body: { status: newStatus as Defect['status'] } });
  };

  const handleSeverityChange = (defectId: string, newSeverity: string) => {
    updateDefect.mutate({ projectId: projectId!, id: defectId, body: { severity: newSeverity as Defect['severity'] } });
  };

  const handleDeleteDefect = () => {
    if (deleteTarget) {
      deleteDefect.mutate(
        { projectId: projectId!, id: deleteTarget },
        { onSuccess: () => setDeleteTarget(null), onError: () => setDeleteTarget(null) }
      );
    }
  };

  const openEdit = (d: Defect) => {
    setEditTarget({ ...d });
    setEditOpen(true);
  };

  const handleEdit = () => {
    if (editTarget) {
      updateDefect.mutate(
        {
          projectId: projectId!,
          id: editTarget.id,
          body: {
            title:       editTarget.title,
            description: editTarget.description,
            severity:    editTarget.severity,
            status:      editTarget.status,
            source:      editTarget.source,
            link:        editTarget.link,
          },
        },
        { onSuccess: () => setEditOpen(false) }
      );
    }
  };

  return (
    <div className="h-full flex flex-col">
      {/* Header */}
      <div className="px-6 pt-5 pb-4 bg-card border-b border-border/40">
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
          <Button size="sm" variant="outline" className="gap-1.5" onClick={() => setDownloadOpen(true)}>
            <Download className="h-3.5 w-3.5" strokeWidth={1.5} /> Export
          </Button>
        </div>
      </div>

      {/* Content — centered canvas */}
      <div className="flex-1 overflow-auto p-6 bg-background flex justify-center">
        <div className="w-full max-w-[1200px] space-y-6 pb-20">

          {/* Executive Summary */}
          {report.sections.executiveSummary && (
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
                <div className="grid grid-cols-[1fr_auto] gap-6 items-stretch">
                  {/* KPI Cards — 2 cols × 3 rows */}
                  <div className="grid grid-cols-2 gap-3">
                    {/* Row 1 */}
                    <div className="bg-background rounded-lg border border-border/40 p-4 flex flex-col items-center justify-center">
                      <p className="text-2xl font-bold text-foreground">{totalCases}</p>
                      <p className="text-[10px] uppercase tracking-widest text-muted-foreground font-semibold mt-1">Total Cases</p>
                    </div>
                    <div className="bg-background rounded-lg border border-border/40 p-4 flex flex-col items-center justify-center">
                      <p className="text-2xl font-bold text-success">{passRate}%</p>
                      <p className="text-[10px] uppercase tracking-widest text-muted-foreground font-semibold mt-1">Pass Rate</p>
                      <p className="text-[9px] text-muted-foreground mt-0.5">(of executed)</p>
                    </div>
                    {/* Row 2 */}
                    <div className="bg-background rounded-lg border border-border/40 p-4 flex flex-col items-center justify-center">
                      <p className="text-2xl font-bold text-destructive">{totalDefects}</p>
                      <p className="text-[10px] uppercase tracking-widest text-muted-foreground font-semibold mt-1">Total Defects</p>
                    </div>
                    <div className="bg-background rounded-lg border border-border/40 p-4 flex flex-col items-center justify-center">
                      <p className="text-2xl font-bold text-[hsl(215,60%,55%)]">{executionProgress.toFixed(1)}%</p>
                      <p className="text-[10px] uppercase tracking-widest text-muted-foreground font-semibold mt-1">Execution Progress</p>
                      <div className="mt-2 h-1 w-full rounded-full bg-border/60 overflow-hidden">
                        <div className="h-full rounded-full bg-[hsl(215,60%,55%)] transition-all duration-500" style={{ width: `${executionProgress}%` }} />
                      </div>
                    </div>
                    {/* Row 3 */}
                    <div className="bg-background rounded-lg border border-border/40 p-4 flex flex-col items-center justify-center">
                      <p className="text-2xl font-bold" style={{ color: COLORS.untested }}>{totalUntested}</p>
                      <p className="text-[10px] uppercase tracking-widest text-muted-foreground font-semibold mt-1">Untested</p>
                    </div>
                    <div className="bg-background rounded-lg border border-border/40 p-4 flex flex-col items-center justify-center">
                      <p className="text-2xl font-bold text-muted-foreground">{remainingScope.toFixed(1)}%</p>
                      <p className="text-[10px] uppercase tracking-widest text-muted-foreground font-semibold mt-1">Remaining Scope</p>
                    </div>
                  </div>
                  {/* Donut Chart + Legend — height matches card grid */}
                  <div className="flex flex-col items-center justify-center gap-4 min-w-[220px]">
                    <div className="w-56 h-56">
                      <ResponsiveContainer width="100%" height="100%">
                        <PieChart>
                          <Pie
                            data={pieData}
                            cx="50%"
                            cy="50%"
                            innerRadius={52}
                            outerRadius={88}
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
                    <div className="flex flex-col gap-2">
                      {pieData.map((d) => {
                        const pct = totalCases > 0 ? ((d.value / totalCases) * 100).toFixed(0) : '0';
                        return (
                          <div key={d.name} className="flex items-center gap-2">
                            <div className="h-2.5 w-2.5 rounded-full shrink-0" style={{ background: d.color }} />
                            <span className="text-xs text-muted-foreground">
                              {d.name}: <span className="font-semibold text-foreground">{d.value}</span>{' '}
                              <span className="text-muted-foreground">({pct}%)</span>
                            </span>
                          </div>
                        );
                      })}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Environment Info */}
          {report.sections.environmentInfo && linkedRuns.length > 0 && (
            <div className="bg-card rounded-xl border border-border/40 shadow-sm overflow-hidden">
              <div className="px-5 py-3.5 border-b border-border/40">
                <h2 className="text-sm font-semibold text-foreground uppercase tracking-wider">Environment Details</h2>
              </div>
              <div className="p-5">
                <div className="grid grid-cols-3 gap-4">
                  <div className="bg-background rounded-lg border border-border/40 p-4">
                    <p className="text-[10px] uppercase tracking-widest text-muted-foreground font-semibold mb-1">Environment(s)</p>
                    <div className="flex flex-wrap gap-1.5">
                      {[...new Set(linkedRuns.map((r) => r.environment))].map((env) => (
                        <span key={env} className="text-xs font-mono bg-secondary px-2 py-0.5 rounded text-foreground">{env}</span>
                      ))}
                    </div>
                  </div>
                  <div className="bg-background rounded-lg border border-border/40 p-4">
                    <p className="text-[10px] uppercase tracking-widest text-muted-foreground font-semibold mb-1">Build Version(s)</p>
                    <div className="flex flex-wrap gap-1.5">
                      {[...new Set(linkedRuns.map((r) => r.buildVersion).filter(Boolean))].map((v) => (
                        <span key={v} className="text-xs font-mono bg-secondary px-2 py-0.5 rounded text-foreground">{v}</span>
                      ))}
                    </div>
                  </div>
                  <div className="bg-background rounded-lg border border-border/40 p-4">
                    <p className="text-[10px] uppercase tracking-widest text-muted-foreground font-semibold mb-1">Created By</p>
                    <p className="text-sm font-medium text-foreground">{report.createdBy}</p>
                    <p className="text-[10px] text-muted-foreground font-mono mt-1">{report.date}</p>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Suite Analysis */}
          {report.sections.suiteAnalytics && (
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
          {report.sections.defectTable && (
            <div className="bg-card rounded-lg shadow-soft overflow-hidden">
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
                  {defects.length === 0 ? (
                    <tr>
                      <td colSpan={7} className="text-center py-12 text-muted-foreground text-sm">No defects linked to this report</td>
                    </tr>
                  ) : (
                    [...defects].sort((a, b) => b.createdAt.localeCompare(a.createdAt)).map((d) => (
                      <tr
                        key={d.id}
                        className="hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-colors border-b border-slate-100 dark:border-slate-700/50 bg-card cursor-pointer"
                        onClick={() => { setViewTarget(d); setViewOpen(true); }}
                      >
                        <td className="px-5 py-3.5 font-mono text-[10px] text-accent tracking-wider" title={d.id}>{defectDisplayIdMap[d.id] ?? '-'}</td>
                        <td className="px-5 py-3.5 font-medium text-foreground">{d.title}</td>
                        <td className="px-5 py-3.5" onClick={(e) => e.stopPropagation()}>
                          <Select value={d.severity} onValueChange={(val) => handleSeverityChange(d.id, val)}>
                            <SelectTrigger className={`h-7 w-auto text-[10px] font-mono uppercase tracking-widest rounded-md px-2.5 ${defectSeverityStyles[d.severity] || ''}`}>
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
                          <Select value={d.status} onValueChange={(val) => handleStatusChange(d.id, val)}>
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
                        </td>
                        <td className="px-5 py-3.5 font-mono text-[10px] text-muted-foreground tracking-wider" title={d.source || undefined}>
                          {d.source ? (isUuid(d.source) ? d.source.slice(0, 8) : d.source) : '—'}
                        </td>
                        <td className="px-5 py-3.5 text-muted-foreground font-mono text-[10px] tracking-wider">{formatDate(d.createdAt)}</td>
                        <td className="px-5 py-3.5" onClick={(e) => e.stopPropagation()}>
                          <div className="flex items-center gap-1">
                            <button
                              onClick={() => { setViewTarget(d); setViewOpen(true); }}
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
                              onClick={() => { setDeleteTarget(d.id); }}
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
      <Dialog open={viewOpen} onOpenChange={setViewOpen}>
        <DialogContent className="sm:max-w-3xl bg-card">
          <DialogHeader>
            <DialogTitle className="text-foreground flex items-center gap-2">
              <span className="font-mono text-purple-600 dark:text-purple-400 text-sm" title={viewTarget?.id}>{viewTarget ? (defectDisplayIdMap[viewTarget.id] ?? '-') : '-'}</span>
              {viewTarget?.title}
            </DialogTitle>
            <DialogDescription className="text-muted-foreground">Defect details</DialogDescription>
          </DialogHeader>
          {viewTarget && (
            <div className="space-y-4">
              <div>
                <label className="text-[10px] font-semibold text-muted-foreground uppercase mb-1.5 block font-mono tracking-widest">Description</label>
                <div className="mt-0 px-3 py-2 bg-white border border-input rounded-md text-sm text-foreground min-h-[140px] resize-none leading-relaxed overflow-y-auto max-h-[200px]">
                  {viewTarget.description || 'No description provided.'}
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-[10px] font-semibold text-muted-foreground uppercase mb-1.5 block font-mono tracking-widest">Source</label>
                  <div className="mt-0 h-9 px-3 bg-white border border-input rounded-md text-sm text-foreground font-mono flex items-center">
                    {viewTarget.source || '—'}
                  </div>
                </div>
                <div>
                  <label className="text-[10px] font-semibold text-muted-foreground uppercase mb-1.5 block font-mono tracking-widest">Created</label>
                  <div className="mt-0 h-9 px-3 bg-white border border-input rounded-md text-sm text-foreground flex items-center">
                    {formatDate(viewTarget.createdAt)}
                  </div>
                </div>
              </div>
              {viewTarget.link && (
                <div>
                  <label className="text-[10px] font-semibold text-muted-foreground uppercase mb-1.5 block font-mono tracking-widest">External Link</label>
                  <a href={viewTarget.link} target="_blank" rel="noopener noreferrer" className="mt-0 h-9 px-3 bg-white border border-input rounded-md text-sm text-purple-600 hover:text-purple-700 flex items-center gap-1 hover:underline">
                    {viewTarget.link} <ExternalLink className="h-3 w-3" />
                  </a>
                </div>
              )}
              <div>
                <label className="text-[10px] font-semibold text-muted-foreground uppercase mb-1.5 block font-mono tracking-widest">Attachments</label>
                <p className="text-sm text-muted-foreground mt-1.5">No attachments</p>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Edit Defect Modal */}
      <Dialog open={editOpen} onOpenChange={setEditOpen}>
        <DialogContent className="sm:max-w-3xl bg-card">
          <DialogHeader>
            <DialogTitle className="text-foreground">Edit Defect</DialogTitle>
            <DialogDescription className="text-muted-foreground font-mono" title={editTarget?.id}>{editTarget ? (defectDisplayIdMap[editTarget.id] ?? '-') : '-'}</DialogDescription>
          </DialogHeader>
          {editTarget && (
            <div className="space-y-4">
              <div>
                <label className="text-[10px] font-semibold text-muted-foreground uppercase mb-1.5 block font-mono tracking-widest">Title</label>
                <Input value={editTarget.title} onChange={(e) => setEditTarget({ ...editTarget, title: e.target.value })} className="mt-0 h-9 bg-white border border-input focus-visible:ring-1 focus-visible:ring-ring" />
              </div>
              <div>
                <label className="text-[10px] font-semibold text-muted-foreground uppercase mb-1.5 block font-mono tracking-widest">Description</label>
                <Textarea value={editTarget.description} onChange={(e) => setEditTarget({ ...editTarget, description: e.target.value })} className="mt-0 min-h-[140px] bg-white border border-input focus-visible:ring-1 focus-visible:ring-ring text-sm resize-none" />
              </div>
              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="text-[10px] font-semibold text-muted-foreground uppercase mb-1.5 block font-mono tracking-widest">Severity</label>
                  <Select value={editTarget.severity} onValueChange={(v) => setEditTarget({ ...editTarget, severity: v as 'Critical' | 'Major' | 'Minor' })}>
                    <SelectTrigger className="mt-0 h-9 bg-white border border-input"><SelectValue /></SelectTrigger>
                    <SelectContent>
                      <SelectItem value="Critical">Critical</SelectItem>
                      <SelectItem value="Major">Major</SelectItem>
                      <SelectItem value="Minor">Minor</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div>
                  <label className="text-[10px] font-semibold text-muted-foreground uppercase mb-1.5 block font-mono tracking-widest">Status</label>
                  <Select value={editTarget.status} onValueChange={(v) => setEditTarget({ ...editTarget, status: v as Defect['status'] })}>
                    <SelectTrigger className="mt-0 h-9 bg-white border border-input"><SelectValue /></SelectTrigger>
                    <SelectContent>
                      <SelectItem value="Open">Open</SelectItem>
                      <SelectItem value="In Progress">In Progress</SelectItem>
                      <SelectItem value="Fixed">Fixed</SelectItem>
                      <SelectItem value="Closed">Closed</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div>
                  <label className="text-[10px] font-semibold text-muted-foreground uppercase mb-1.5 block font-mono tracking-widest">Source</label>
                  <Input value={editTarget.source || ''} onChange={(e) => setEditTarget({ ...editTarget, source: e.target.value })} placeholder="TR-01 or AS-1" className="mt-0 h-9 bg-white border border-input focus-visible:ring-1 focus-visible:ring-ring" />
                </div>
              </div>
              <div>
                <label className="text-[10px] font-semibold text-muted-foreground uppercase mb-1.5 block font-mono tracking-widest">External Link</label>
                <Input value={editTarget.link} onChange={(e) => setEditTarget({ ...editTarget, link: e.target.value })} className="mt-0 h-9 bg-white border border-input focus-visible:ring-1 focus-visible:ring-ring" />
              </div>
            </div>
          )}
          <DialogFooter className="mt-4">
            <Button variant="ghost" onClick={() => setEditOpen(false)}>Cancel</Button>
            <Button onClick={handleEdit}>Save Changes</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation */}
      <Dialog open={deleteTarget !== null} onOpenChange={(open) => { if (!open) setDeleteTarget(null); }}>
        <DialogContent className="sm:max-w-sm bg-card">
          <DialogHeader>
            <DialogTitle className="text-foreground flex items-center gap-3">
              <AlertTriangle className="h-6 w-6 text-destructive shrink-0" strokeWidth={1.5} />
              Remove Defect
            </DialogTitle>
            <DialogDescription className="text-sm text-foreground mt-3">
              Are you sure you want to remove <span className="font-bold">{deleteTarget}</span> from this report? This will unlink it from the current view.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter className="mt-6">
            <Button variant="outline" onClick={() => setDeleteTarget(null)}>Cancel</Button>
            <Button variant="destructive" onClick={handleDeleteDefect}>Remove</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Download modal */}
      <Dialog open={downloadOpen} onOpenChange={setDownloadOpen}>
        <DialogContent className="sm:max-w-sm bg-card rounded-xl">
          <DialogHeader>
            <DialogTitle className="text-base">Export Report</DialogTitle>
            <DialogDescription>Choose a format</DialogDescription>
          </DialogHeader>
          <div className="grid grid-cols-2 gap-3 py-2">
            <button onClick={() => handleDownload('pdf')} className="flex flex-col items-center gap-2 p-4 rounded-lg border border-border hover:border-accent hover:bg-accent/5 transition-colors">
              <FileText className="h-8 w-8 text-accent" strokeWidth={1.5} />
              <span className="text-sm font-medium text-foreground">PDF</span>
              <span className="text-[10px] text-muted-foreground">Visual summary</span>
            </button>
            <button onClick={() => handleDownload('csv')} className="flex flex-col items-center gap-2 p-4 rounded-lg border border-border hover:border-accent hover:bg-accent/5 transition-colors">
              <Table2 className="h-8 w-8 text-accent" strokeWidth={1.5} />
              <span className="text-sm font-medium text-foreground">Excel / CSV</span>
              <span className="text-[10px] text-muted-foreground">Raw data table</span>
            </button>
          </div>
        </DialogContent>
      </Dialog>

    </div>
  );
};

export default ReportDetail;
