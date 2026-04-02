import { useState } from 'react';
import { Search, Grid, List, FileText, Image, File, Download, Trash2, AlertTriangle, Paperclip } from 'lucide-react';
import Breadcrumbs from '@/components/Breadcrumbs';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { useParams } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { attachmentsApi, type Attachment } from '@/lib/api';
import { useProject } from '@/hooks/useApi';
import { toast } from 'sonner';
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription,
} from '@/components/ui/dialog';
import { formatDate } from '@/lib/utils';

// ── helpers ────────────────────────────────────────────────────────────────────

const BASE_URL = import.meta.env.VITE_API_URL ??
  (import.meta.env.DEV ? 'http://localhost:8080/api' : '/api');

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1048576) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1048576).toFixed(1)} MB`;
}

function fileIconComponent(mimeType: string | null | undefined) {
  if (!mimeType) return File;
  if (mimeType.startsWith('image/')) return Image;
  if (mimeType.startsWith('text/') || mimeType.includes('json') || mimeType.includes('xml')) return FileText;
  return File;
}

function isPreviewable(mimeType: string | null | undefined): boolean {
  return !!mimeType && mimeType.startsWith('image/');
}

// ── component ──────────────────────────────────────────────────────────────────

const Attachments = () => {
  const { projectId } = useParams<{ projectId: string }>();
  const qc = useQueryClient();

  const { data: project } = useProject(projectId!);
  const [viewMode, setViewMode] = useState<'table' | 'grid'>('table');
  const [search, setSearch] = useState('');
  const [previewFile, setPreviewFile] = useState<Attachment | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Attachment | null>(null);

  // ── queries ──────────────────────────────────────────────────────────────────

  const { data: rawList, isLoading } = useQuery({
    queryKey: ['attachments', projectId],
    queryFn:  () => attachmentsApi.list(projectId!),
    enabled:  !!projectId,
  });

  // backend returns { data: [...] } (PageResult), unwrap it
  const attachments: Attachment[] = Array.isArray(rawList)
    ? rawList
    : ((rawList as { data?: Attachment[] })?.data ?? []);

  const filtered = attachments
    .filter(f => f.fileName?.toLowerCase().includes(search.toLowerCase()))
    .sort((a, b) => (b.uploadedAt ?? '').localeCompare(a.uploadedAt ?? ''));

  // ── mutations ─────────────────────────────────────────────────────────────────

  const deleteMut = useMutation({
    mutationFn: (id: string) => attachmentsApi.delete(projectId!, id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['attachments', projectId] });
      toast.success('Attachment deleted');
      setDeleteTarget(null);
    },
    onError: (e) => toast.error(e instanceof Error ? e.message : 'Delete failed'),
  });

  const downloadUrl = (att: Attachment) =>
    `${BASE_URL}/projects/${projectId}/attachments/${att.id}/content`;

  // ── render ────────────────────────────────────────────────────────────────────

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
                { label: 'Attachments' },
              ]} />
            </div>
            <h1 className="text-xl font-bold text-foreground tracking-[-0.02em]">Attachments</h1>
          </div>

          {/* Filter Bar */}
          <div className="py-3 mb-4 flex items-center gap-3">
            <div className="relative flex-1 max-w-xs">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" strokeWidth={1.5} />
              <Input
                placeholder="Search files..."
                value={search}
                onChange={e => setSearch(e.target.value)}
                className="pl-9 h-9 bg-card border border-border focus-visible:ring-1 focus-visible:ring-accent/40"
              />
            </div>
            <div className="ml-auto flex items-center gap-1">
              <Button variant={viewMode === 'grid' ? 'secondary' : 'ghost'} size="icon" className="h-8 w-8" onClick={() => setViewMode('grid')}>
                <Grid className="h-4 w-4" strokeWidth={1.5} />
              </Button>
              <Button variant={viewMode === 'table' ? 'secondary' : 'ghost'} size="icon" className="h-8 w-8" onClick={() => setViewMode('table')}>
                <List className="h-4 w-4" strokeWidth={1.5} />
              </Button>
            </div>
          </div>

          {/* Loading */}
          {isLoading && (
            <div className="flex items-center justify-center py-20 text-muted-foreground text-sm">Loading…</div>
          )}

          {/* Empty state */}
          {!isLoading && filtered.length === 0 && (
            <div className="flex flex-col items-center justify-center py-20 gap-3 text-muted-foreground">
              <Paperclip className="h-10 w-10 opacity-30" strokeWidth={1} />
              <p className="text-sm">{search ? 'No files match your search.' : 'No attachments found.'}</p>
            </div>
          )}

          {/* Table View */}
          {!isLoading && filtered.length > 0 && viewMode === 'table' && (
            <div className="bg-card rounded-lg shadow-soft overflow-hidden">
              <table className="w-full text-sm">
                <thead>
                  <tr className="bg-slate-200 dark:bg-slate-700 sticky top-0 z-10">
                    <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">File Name</th>
                    <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Size</th>
                    <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Type</th>
                    <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Uploaded</th>
                    <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.map((f) => {
                    const Icon = fileIconComponent(f.fileType);
                    return (
                      <tr key={f.id} className="hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-colors border-b border-slate-100 dark:border-slate-700/50">
                        <td className="px-5 py-3.5">
                          <button
                            className="flex items-center gap-2 cursor-pointer text-left w-full"
                            onClick={() => isPreviewable(f.fileType) ? setPreviewFile(f) : window.open(downloadUrl(f), '_blank')}
                          >
                            <Icon className="h-4 w-4 text-muted-foreground shrink-0" strokeWidth={1.5} />
                            <span className="font-medium text-foreground hover:text-primary transition-colors">{f.fileName}</span>
                          </button>
                        </td>
                        <td className="px-5 py-3.5 text-muted-foreground font-mono text-xs">{formatSize(f.fileSize)}</td>
                        <td className="px-5 py-3.5 text-muted-foreground text-xs truncate max-w-[140px]">{f.fileType ?? '—'}</td>
                        <td className="px-5 py-3.5 text-muted-foreground font-mono text-[10px] tracking-wider">{formatDate(f.uploadedAt)}</td>
                        <td className="px-5 py-3.5">
                          <div className="flex items-center gap-1">
                            <a href={downloadUrl(f)} download={f.fileName} onClick={e => e.stopPropagation()}>
                              <Button variant="ghost" size="icon" className="h-7 w-7">
                                <Download className="h-3.5 w-3.5 text-muted-foreground" strokeWidth={1.5} />
                              </Button>
                            </a>
                            <Button variant="ghost" size="icon" className="h-7 w-7" onClick={() => setDeleteTarget(f)}>
                              <Trash2 className="h-3.5 w-3.5 text-destructive" strokeWidth={1.5} />
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

          {/* Grid View */}
          {!isLoading && filtered.length > 0 && viewMode === 'grid' && (
            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
              {filtered.map((f) => {
                const Icon = fileIconComponent(f.fileType);
                const preview = isPreviewable(f.fileType);
                return (
                  <div key={f.id} className="rounded-lg border border-border bg-card overflow-hidden group">
                    <button
                      className="block w-full cursor-pointer"
                      onClick={() => preview ? setPreviewFile(f) : window.open(downloadUrl(f), '_blank')}
                    >
                      <div className="aspect-[4/3] bg-muted/30 flex items-center justify-center overflow-hidden">
                        {preview ? (
                          <img src={downloadUrl(f)} alt={f.fileName} className="w-full h-full object-cover" />
                        ) : (
                          <Icon className="h-12 w-12 text-muted-foreground/40" strokeWidth={1} />
                        )}
                      </div>
                    </button>
                    <div className="p-3 space-y-1">
                      <p className="text-sm font-medium text-foreground truncate">{f.fileName}</p>
                      <p className="text-[10px] font-mono text-muted-foreground tracking-wider">{formatSize(f.fileSize)}</p>
                      <div className="flex items-center justify-between pt-1">
                        <span className="text-xs text-muted-foreground">{formatDate(f.uploadedAt)}</span>
                        <div className="flex items-center gap-0.5">
                          <a href={downloadUrl(f)} download={f.fileName} onClick={e => e.stopPropagation()}>
                            <Button variant="ghost" size="icon" className="h-6 w-6">
                              <Download className="h-3 w-3 text-muted-foreground" strokeWidth={1.5} />
                            </Button>
                          </a>
                          <Button variant="ghost" size="icon" className="h-6 w-6" onClick={() => setDeleteTarget(f)}>
                            <Trash2 className="h-3 w-3 text-destructive" strokeWidth={1.5} />
                          </Button>
                        </div>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>

      {/* Preview Modal */}
      <Dialog open={!!previewFile} onOpenChange={() => setPreviewFile(null)}>
        <DialogContent className="sm:max-w-2xl bg-background rounded-xl">
          <DialogHeader>
            <DialogTitle className="text-base font-semibold">{previewFile?.fileName}</DialogTitle>
            <DialogDescription className="text-xs text-muted-foreground">
              {previewFile && formatSize(previewFile.fileSize)} · {previewFile && formatDate(previewFile.uploadedAt)}
            </DialogDescription>
          </DialogHeader>
          <div className="flex items-center justify-center min-h-[300px] bg-muted/20 rounded-lg overflow-hidden">
            {previewFile && isPreviewable(previewFile.fileType) ? (
              <img src={downloadUrl(previewFile)} alt={previewFile.fileName} className="max-w-full max-h-[60vh] object-contain" />
            ) : (
              <div className="flex flex-col items-center gap-3 text-muted-foreground">
                <FileText className="h-16 w-16" strokeWidth={1} />
                <p className="text-sm">Preview not available for this file type</p>
              </div>
            )}
          </div>
          <DialogFooter>
            <Button variant="ghost" onClick={() => setPreviewFile(null)}>Close</Button>
            {previewFile && (
              <a href={downloadUrl(previewFile)} download={previewFile.fileName}>
                <Button className="gap-1.5">
                  <Download className="h-3.5 w-3.5" /> Download
                </Button>
              </a>
            )}
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation Modal */}
      <Dialog open={!!deleteTarget} onOpenChange={() => setDeleteTarget(null)}>
        <DialogContent className="sm:max-w-sm bg-card">
          <DialogHeader>
            <DialogTitle className="text-foreground flex items-center gap-3">
              <AlertTriangle className="h-6 w-6 text-destructive shrink-0" strokeWidth={1.5} />
              Delete Attachment
            </DialogTitle>
            <DialogDescription className="text-sm text-foreground mt-3">
              Are you sure you want to delete <span className="font-bold">{deleteTarget?.fileName}</span>? This action cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter className="mt-6">
            <Button variant="outline" onClick={() => setDeleteTarget(null)}>Cancel</Button>
            <Button
              variant="destructive"
              disabled={deleteMut.isPending}
              onClick={() => deleteTarget && deleteMut.mutate(deleteTarget.id)}
            >
              {deleteMut.isPending ? 'Deleting…' : 'Delete'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default Attachments;
