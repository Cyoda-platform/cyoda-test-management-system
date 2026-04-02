import { useState } from 'react';
import { Plus, Eye, Download, Trash2, AlertTriangle, FileText, Table2 } from 'lucide-react';
import Breadcrumbs from '@/components/Breadcrumbs';
import { Button } from '@/components/ui/button';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from '@/components/ui/dialog';
import { useProject } from '@/hooks/useApi';
import { formatDate } from '@/lib/utils';

export interface MockReport {
  id: string;
  name: string;
  type: 'Summary' | 'Regression' | 'Sprint' | 'Custom';
  createdBy: string;
  date: string;
}


const typeBadge: Record<string, string> = {
  Summary: 'text-success',
  Regression: 'text-accent',
  Sprint: 'text-warning',
  Custom: 'text-muted-foreground',
};

const Reports = () => {
  const { projectId } = useParams<{ projectId: string }>();
  const navigate = useNavigate();
  const { data: project } = useProject(projectId!);

  const [reports, setReports] = useState<MockReport[]>(() => {
    const reportsKey = `reports-${projectId}`;
    const saved = localStorage.getItem(reportsKey);
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        if (Array.isArray(parsed)) return parsed;
      } catch {
        // ignore parse errors
      }
    }
    return [];
  });
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<MockReport | null>(null);
  const [downloadOpen, setDownloadOpen] = useState(false);
  const [downloadTarget, setDownloadTarget] = useState<MockReport | null>(null);

  const handleDelete = () => {
    if (deleteTarget) {
      setReports((prev) => {
        const filtered = prev.filter((r) => r.id !== deleteTarget.id);
        localStorage.setItem(`reports-${projectId}`, JSON.stringify(filtered));
        return filtered;
      });
      setDeleteOpen(false);
      setDeleteTarget(null);
    }
  };

  const handleDownload = (format: string) => {
    if (downloadTarget) {
      const blob = new Blob([`${downloadTarget.name} — exported as ${format}`], { type: 'text/plain' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${downloadTarget.name.replace(/\s+/g, '_')}.${format.toLowerCase()}`;
      a.click();
      URL.revokeObjectURL(url);
      setDownloadOpen(false);
      setDownloadTarget(null);
    }
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
                { label: project?.name || 'Project', href: `/projects/${projectId}/repository` },
                { label: 'Reports' },
              ]} />
            </div>
            <div className="flex items-center justify-between">
              <h1 className="text-xl font-bold text-foreground tracking-[-0.02em]">Reports</h1>
              <Button
                size="sm"
                className="bg-gradient-to-br from-primary to-primary/80 text-primary-foreground gap-1.5 border-0"
                onClick={() => navigate(`/projects/${projectId}/reports/create`)}
              >
                <Plus className="h-3.5 w-3.5" strokeWidth={1.5} /> Create Report
              </Button>
            </div>
          </div>

          {/* Table */}
          <div className="bg-card rounded-lg shadow-soft overflow-hidden">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-slate-200 dark:bg-slate-700 sticky top-0 z-10">
              <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">ID</th>
              <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Report Name</th>
              <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Type</th>
              <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Created By</th>
              <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Date</th>
              <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Actions</th>
            </tr>
          </thead>
          <tbody>
            {[...reports].sort((a, b) => b.date.localeCompare(a.date)).map((r) => (
              <tr key={r.id} className="hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-colors border-b border-slate-100 dark:border-slate-700/50 bg-card">
                <td className="px-5 py-3.5 font-mono text-[10px] text-muted-foreground tracking-wider">{r.id}</td>
                <td className="px-5 py-3.5 font-medium text-foreground">
                  <Link
                    to={`/projects/${projectId}/reports/${r.id}`}
                  >
                    {r.name}
                  </Link>
                </td>
                <td className="px-5 py-3.5">
                  <span className={`${typeBadge[r.type] || 'text-muted-foreground'} text-[10px] px-2.5 py-0.5 font-mono uppercase tracking-widest inline-flex items-center gap-1`}>
                    {r.type}
                  </span>
                </td>
                <td className="px-5 py-3.5 text-muted-foreground">{r.createdBy}</td>
                <td className="px-5 py-3.5 text-muted-foreground font-mono text-[10px] tracking-wider">{formatDate(r.date)}</td>
                <td className="px-5 py-3.5">
                  <div className="flex items-center gap-1">
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-8 w-8 text-muted-foreground hover:text-foreground"
                      onClick={() => navigate(`/projects/${projectId}/reports/${r.id}`)}
                    >
                      <Eye className="h-4 w-4" strokeWidth={1.5} />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-8 w-8 text-muted-foreground hover:text-foreground"
                      onClick={() => { setDownloadTarget(r); setDownloadOpen(true); }}
                    >
                      <Download className="h-4 w-4" strokeWidth={1.5} />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-8 w-8 text-muted-foreground hover:text-destructive"
                      onClick={() => { setDeleteTarget(r); setDeleteOpen(true); }}
                    >
                      <Trash2 className="h-4 w-4" strokeWidth={1.5} />
                    </Button>
                  </div>
                </td>
              </tr>
            ))}
            {reports.length === 0 && (
              <tr><td colSpan={6} className="text-center py-12 text-muted-foreground">No reports yet.</td></tr>
            )}
          </tbody>
        </table>
          </div>
        </div>
      </div>

      {/* Delete confirmation */}
      <Dialog open={deleteOpen} onOpenChange={setDeleteOpen}>
        <DialogContent className="sm:max-w-sm bg-card">
          <DialogHeader>
            <DialogTitle className="text-foreground flex items-center gap-3">
              <AlertTriangle className="h-6 w-6 text-destructive shrink-0" strokeWidth={1.5} />
              Delete Report
            </DialogTitle>
            <DialogDescription className="text-sm text-foreground mt-3">
              Are you sure you want to delete <span className="font-bold">{deleteTarget?.name}</span>? This action cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter className="mt-6">
            <Button variant="outline" onClick={() => setDeleteOpen(false)}>Cancel</Button>
            <Button variant="destructive" onClick={handleDelete}>Delete</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Download format picker */}
      <Dialog open={downloadOpen} onOpenChange={setDownloadOpen}>
        <DialogContent className="sm:max-w-sm bg-card rounded-xl">
          <DialogHeader>
            <DialogTitle className="text-base">Export Report</DialogTitle>
            <DialogDescription>Choose a format for <span className="font-semibold text-foreground">{downloadTarget?.name}</span></DialogDescription>
          </DialogHeader>
          <div className="grid grid-cols-2 gap-3 py-2">
            <button
              onClick={() => handleDownload('pdf')}
              className="flex flex-col items-center gap-2 p-4 rounded-lg border border-border hover:border-accent hover:bg-accent/5 transition-colors"
            >
              <FileText className="h-8 w-8 text-accent" strokeWidth={1.5} />
              <span className="text-sm font-medium text-foreground">PDF</span>
              <span className="text-[10px] text-muted-foreground">Visual summary with charts</span>
            </button>
            <button
              onClick={() => handleDownload('csv')}
              className="flex flex-col items-center gap-2 p-4 rounded-lg border border-border hover:border-accent hover:bg-accent/5 transition-colors"
            >
              <Table2 className="h-8 w-8 text-accent" strokeWidth={1.5} />
              <span className="text-sm font-medium text-foreground">Excel / CSV</span>
              <span className="text-[10px] text-muted-foreground">Raw data table export</span>
            </button>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default Reports;
