import { useState, useMemo } from 'react';
import Breadcrumbs from '@/components/Breadcrumbs';
import { useParams, useNavigate } from 'react-router-dom';
import { Plus, ExternalLink, Trash2, Lock, Search, AlertTriangle, Pencil, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from '@/components/ui/dialog';
import { Label } from '@/components/ui/label';
import { toast } from 'sonner';
import { useProject, useTestRuns, useUpdateTestRun, useDeleteTestRun } from '@/hooks/useApi';
import type { TestRun } from '@/lib/api';
import { listDisplayId } from '@/lib/utils';

const StatusBadge = ({ status }: { status: string }) => {
  const cls = status === 'initial' ? 'badge-status-initial' : status === 'active' ? 'badge-status-active' : 'badge-status-completed';
  return (
    <span className={`${cls} inline-flex items-center gap-1`}>
      {status === 'completed' && <Lock className="h-3 w-3" strokeWidth={1.5} />}
      {status.charAt(0).toUpperCase() + status.slice(1)}
    </span>
  );
};

const ProgressBar = ({ passed = 0, failed = 0, untested = 0 }: { passed?: number; failed?: number; untested?: number }) => {
  const p = passed ?? 0;
  const f = failed ?? 0;
  const u = untested ?? 0;
  const total = p + f + u;
  if (total === 0) return <div className="h-1.5 rounded-full bg-secondary w-full" />;
  return (
    <div className="flex h-1.5 rounded-full overflow-hidden w-full bg-secondary" title={`Passed: ${p} | Failed: ${f} | Untested: ${u}`}>
      {p > 0 && <div className="bg-success" style={{ width: `${(p / total) * 100}%` }} />}
      {f > 0 && <div className="bg-destructive" style={{ width: `${(f / total) * 100}%` }} />}
      {u > 0 && <div className="bg-muted-foreground/20" style={{ width: `${(u / total) * 100}%` }} />}
    </div>
  );
};

const TestRuns = () => {
  const { projectId } = useParams<{ projectId: string }>();
  const navigate = useNavigate();

  // Live data from API
  const { data: project } = useProject(projectId!);
  const { data: runs = [], isLoading, isError, error } = useTestRuns(projectId!);

  const updateTestRun = useUpdateTestRun();
  const deleteTestRun = useDeleteTestRun();

  const [statusFilter, setStatusFilter] = useState('all');
  const [envFilter, setEnvFilter] = useState('all');
  const [search, setSearch] = useState('');

  // Edit modal
  const [editOpen, setEditOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<TestRun | null>(null);

  // Delete modal
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<TestRun | null>(null);

  // Build a stable display-ID map (TR-01, TR-02…) ordered by creation time
  const runDisplayIdMap = useMemo(() => {
    const sorted = [...runs].filter(Boolean).sort((a, b) => (a.createdAt || '').localeCompare(b.createdAt || ''));
    const map: Record<string, string> = {};
    sorted.forEach((r, i) => { map[r.id] = listDisplayId('TR', i); });
    return map;
  }, [runs]);

  const filtered = runs
    .filter((r) => {
      if (!r) return false;
      if (statusFilter !== 'all' && r.status !== statusFilter) return false;
      if (envFilter !== 'all' && r.environment !== envFilter) return false;
      if (search && !r.name?.toLowerCase().includes(search.toLowerCase()) && !r.id?.toLowerCase().includes(search.toLowerCase())) return false;
      return true;
    })
    .sort((a, b) => {
      const dateA = a?.createdAt || '';
      const dateB = b?.createdAt || '';
      return dateB.localeCompare(dateA);
    });

  const openCreate = () => {
    navigate(`/projects/${projectId}/runs/create`);
  };

  const handleEdit = () => {
    if (!editTarget) return;
    updateTestRun.mutate(
      {
        projectId: projectId!,
        id: editTarget.id,
        body: {
          name: editTarget.name,
          environment: editTarget.environment,
          buildVersion: editTarget.buildVersion,
          description: editTarget.description,
        },
      },
      {
        onSuccess: () => {
          toast.success('Test run updated');
          setEditOpen(false);
          setEditTarget(null);
        },
        onError: (e) => toast.error(e.message),
      }
    );
  };

  const openEdit = (run: TestRun) => {
    setEditTarget({ ...run });
    setEditOpen(true);
  };

  const handleDelete = () => {
    if (!deleteTarget) return;
    deleteTestRun.mutate(
      { projectId: projectId!, id: deleteTarget.id },
      {
        onSuccess: () => {
          toast.success('Test run deleted');
          setDeleteOpen(false);
          setDeleteTarget(null);
        },
        onError: (e) => toast.error(e.message),
      }
    );
  };

  return (
    <div className="h-full flex flex-col">
      <div className="flex-1 overflow-auto surface-base">
        <div className="max-w-7xl mx-auto w-full px-6 py-6">
          {/* Breadcrumbs & Title */}
          <div className="mb-4">
            <div className="mb-2">
              <Breadcrumbs segments={[
                { label: 'Projects', href: '/projects' },
                { label: project?.name ?? 'Loading...', href: `/projects/${projectId}/repository` },
                { label: 'Test Runs' },
              ]} />
            </div>
            <div className="flex items-center justify-between">
              <h1 className="text-xl font-bold text-foreground tracking-[-0.02em]">Test Runs</h1>
              <Button size="sm" className="bg-gradient-to-br from-primary to-primary/80 text-primary-foreground gap-1.5 border-0" onClick={openCreate}>
                <Plus className="h-3.5 w-3.5" strokeWidth={1.5} /> Create Test Run
              </Button>
            </div>
          </div>

          {/* Filters */}
          <div className="py-3 mb-4 flex items-center gap-3">
            <div className="relative flex-1 max-w-xs">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" strokeWidth={1.5} />
              <Input
                placeholder="Filter by Run name or ID"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="pl-9 h-9 bg-card border border-border focus-visible:ring-1 focus-visible:ring-accent/40"
              />
            </div>
            <Select value={statusFilter} onValueChange={setStatusFilter}>
              <SelectTrigger className="w-36 h-9 bg-card border border-border"><SelectValue placeholder="Status" /></SelectTrigger>
              <SelectContent className="glass-surface border-0">
                <SelectItem value="all">All Status</SelectItem>
                <SelectItem value="initial">Initial</SelectItem>
                <SelectItem value="active">Active</SelectItem>
                <SelectItem value="completed">Completed</SelectItem>
              </SelectContent>
            </Select>
            <Select value={envFilter} onValueChange={setEnvFilter}>
              <SelectTrigger className="w-36 h-9 bg-card border border-border"><SelectValue placeholder="Environment" /></SelectTrigger>
              <SelectContent className="glass-surface border-0">
                <SelectItem value="all">All Envs</SelectItem>
                <SelectItem value="Staging">Staging</SelectItem>
                <SelectItem value="Production">Production</SelectItem>
                <SelectItem value="QA-Env">QA-Env</SelectItem>
                <SelectItem value="Dev">Dev</SelectItem>
              </SelectContent>
            </Select>
          </div>

          {/* Loading state */}
          {isLoading && (
            <div className="flex items-center justify-center py-20 text-muted-foreground gap-2">
              <Loader2 className="h-5 w-5 animate-spin" />
              <span className="text-sm">Loading test runs…</span>
            </div>
          )}

          {/* Error state */}
          {isError && (
            <div className="rounded-lg border border-destructive/30 bg-destructive/10 text-destructive px-5 py-4 text-sm flex items-center gap-2">
              <AlertTriangle className="h-4 w-4 shrink-0" strokeWidth={1.5} />
              {(error as Error)?.message ?? 'Failed to load test runs.'}
            </div>
          )}

          {/* Empty state */}
          {!isLoading && !isError && filtered.length === 0 && (
            <div className="flex flex-col items-center justify-center py-20 gap-3 text-muted-foreground">
              <p className="text-sm">No test runs yet.</p>
              <Button size="sm" className="bg-gradient-to-br from-primary to-primary/80 text-primary-foreground border-0 gap-1.5" onClick={openCreate}>
                <Plus className="h-3.5 w-3.5" strokeWidth={1.5} /> Create Test Run
              </Button>
            </div>
          )}

          {/* Table */}
          {!isLoading && !isError && filtered.length > 0 && (
          <div className="bg-card rounded-lg shadow-soft overflow-hidden">
            <table className="w-full text-sm">
          <thead>
            <tr className="bg-slate-200 dark:bg-slate-700 sticky top-0 z-10">
              <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">ID</th>
              <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Name</th>
              <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Environment</th>
              <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Status</th>
              <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider w-40">Progress</th>
              <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Created</th>
              <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Actions</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((run) => {
              if (!run) return null;
              return (
              <tr key={run.id} className="hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-colors border-b border-slate-100 dark:border-slate-700/50">
                <td className="px-5 py-3.5 font-mono text-[10px] text-muted-foreground tracking-wider" title={run.id ?? undefined}>{runDisplayIdMap[run.id] ?? '-'}</td>
                <td className="px-5 py-3.5 font-medium text-foreground">
                  <button
                    className="hover:text-primary transition-colors text-left cursor-pointer"
                    onClick={() => navigate(`/projects/${projectId}/runs/${run.id}`)}
                  >
                    {run.name ?? 'Unnamed Run'}
                  </button>
                </td>
                <td className="px-5 py-3.5 text-muted-foreground">{run.environment ?? '-'}</td>
                <td className="px-5 py-3.5"><StatusBadge status={run.status ?? 'initial'} /></td>
                <td className="px-5 py-3.5">
                  <ProgressBar passed={run.passed ?? 0} failed={run.failed ?? 0} untested={run.untested ?? 0} />
                </td>
                <td className="px-5 py-3.5 text-muted-foreground whitespace-nowrap text-[10px] font-mono tracking-wider">{run.createdAt ?? '-'}</td>
                <td className="px-5 py-3.5">
                  <div className="flex items-center gap-1">
                    <Button variant="ghost" size="icon" className="h-8 w-8 text-muted-foreground hover:text-foreground"
                      onClick={() => navigate(`/projects/${projectId}/runs/${run.id}`)}>
                      <ExternalLink className="h-4 w-4" strokeWidth={1.5} />
                    </Button>
                    <Button variant="ghost" size="icon" className="h-8 w-8 text-muted-foreground hover:text-foreground"
                      onClick={() => openEdit(run)}>
                      <Pencil className="h-4 w-4" strokeWidth={1.5} />
                    </Button>
                    <Button variant="ghost" size="icon" className="h-8 w-8 text-muted-foreground hover:text-destructive"
                      onClick={() => { setDeleteTarget(run); setDeleteOpen(true); }}>
                      <Trash2 className="h-4 w-4" strokeWidth={1.5} />
                    </Button>
                  </div>
                </td>
              </tr>
            );
            })}
          </tbody>
        </table>
          </div>
          )}
        </div>
      </div>


      {/* Edit Test Run Modal */}
      <Dialog open={editOpen} onOpenChange={setEditOpen}>
        <DialogContent className="sm:max-w-[600px] bg-card border-0 shadow-elevated rounded-xl p-0 gap-0">
          <DialogHeader className="px-6 pt-6 pb-4">
            <DialogTitle className="text-lg font-bold text-foreground tracking-[-0.02em]">Edit Test Run</DialogTitle>
            <DialogDescription className="text-sm text-muted-foreground">
              Update test run details.
            </DialogDescription>
          </DialogHeader>

          {editTarget && (
          <div className="px-6 pb-4 space-y-4">
            <div>
              <Label className="text-xs font-semibold text-muted-foreground uppercase tracking-wider font-mono">Name</Label>
              <Input
                value={editTarget?.name ?? ''}
                onChange={(e) => setEditTarget(t => t ? { ...t, name: e.target.value } : t)}
                placeholder="e.g. Sprint 12 Regression"
                className="mt-1.5 h-9 bg-secondary border-0 focus-visible:ring-1 focus-visible:ring-accent/40"
              />
            </div>

            <div>
              <Label className="text-xs font-semibold text-muted-foreground uppercase tracking-wider font-mono">Environment</Label>
              <Select value={editTarget?.environment ?? 'Staging'} onValueChange={(v) => setEditTarget(t => t ? { ...t, environment: v } : t)}>
                <SelectTrigger className="mt-1.5 h-9 bg-secondary border-0 focus-visible:ring-1 focus-visible:ring-accent/40">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent className="glass-surface border-0">
                  <SelectItem value="Staging">Staging</SelectItem>
                  <SelectItem value="Production">Production</SelectItem>
                  <SelectItem value="QA-Env">QA-Env</SelectItem>
                  <SelectItem value="Dev">Dev</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div>
              <Label className="text-xs font-semibold text-muted-foreground uppercase tracking-wider font-mono">Build / Version</Label>
              <Input
                value={editTarget?.buildVersion ?? ''}
                onChange={(e) => setEditTarget(t => t ? { ...t, buildVersion: e.target.value } : t)}
                placeholder="e.g. v2.4.0-rc1"
                className="mt-1.5 h-9 bg-secondary border-0 focus-visible:ring-1 focus-visible:ring-accent/40"
              />
            </div>

            <div>
              <Label className="text-xs font-semibold text-muted-foreground uppercase tracking-wider font-mono">Description</Label>
              <Textarea
                value={editTarget?.description ?? ''}
                onChange={(e) => setEditTarget(t => t ? { ...t, description: e.target.value } : t)}
                placeholder="Describe the purpose of this test run..."
                className="mt-1.5 bg-secondary border-0 focus-visible:ring-1 focus-visible:ring-accent/40 min-h-[70px]"
              />
            </div>
          </div>
          )}

          <DialogFooter className="px-6 py-4 border-t border-border">
            <Button variant="ghost" onClick={() => setEditOpen(false)}>Cancel</Button>
            <Button
              className="bg-gradient-to-br from-primary to-primary/80 text-primary-foreground border-0"
              onClick={handleEdit}
            >
              Save Changes
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation Modal */}
      <Dialog open={deleteOpen} onOpenChange={setDeleteOpen}>
        <DialogContent className="sm:max-w-sm bg-card">
          <DialogHeader>
            <DialogTitle className="text-foreground flex items-center gap-3">
              <AlertTriangle className="h-6 w-6 text-destructive shrink-0" strokeWidth={1.5} />
              Delete Test Run
            </DialogTitle>
            <DialogDescription className="text-sm text-foreground mt-3">
              Are you sure you want to delete <span className="font-bold">"{deleteTarget?.name}"</span>? This action cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter className="mt-6">
            <Button variant="outline" onClick={() => { setDeleteOpen(false); setDeleteTarget(null); }}>Cancel</Button>
            <Button variant="destructive" onClick={handleDelete}>Delete</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default TestRuns;
