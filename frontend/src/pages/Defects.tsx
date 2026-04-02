import { useState, useRef, useMemo } from 'react';
import { useParams, Link } from 'react-router-dom';
import { Plus, Eye, Pencil, Trash2, ExternalLink, Search, Lock, Upload, FileText, Image, File, X, AlertTriangle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import Breadcrumbs from '@/components/Breadcrumbs';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@/components/ui/dialog';
import { Textarea } from '@/components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { toast } from 'sonner';
import { useProject, useDefects, useCreateDefect, useUpdateDefect, useDeleteDefect } from '@/hooks/useApi';
import type { Defect } from '@/lib/api';
import { Loader2 } from 'lucide-react';
import { listDisplayId, formatDate, isUuid } from '@/lib/utils';

const labelCls = 'text-[10px] font-semibold text-muted-foreground uppercase mb-1.5 block font-mono tracking-widest';

interface AttachFile { name: string; size: number; type: string; }

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

const Defects = () => {
  const { projectId } = useParams<{ projectId: string }>();

  // Live data
  const { data: project } = useProject(projectId!);
  const { data: defects = [], isLoading, isError, error } = useDefects(projectId!);
  const createDefect  = useCreateDefect();
  const updateDefect  = useUpdateDefect();
  const deleteDefect  = useDeleteDefect();

  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('all');

  // Create modal
  const [createOpen, setCreateOpen] = useState(false);
  const [formTitle, setFormTitle] = useState('');
  const [formDesc, setFormDesc] = useState('');
  const [formSeverity, setFormSeverity] = useState<'Critical' | 'Major' | 'Minor'>('Major');
  const [formStatus, setFormStatus] = useState<'Open' | 'In Progress' | 'Fixed' | 'Closed'>('Open');
  const [formLink, setFormLink] = useState('');
  const [formSource, setFormSource] = useState('');
  const [formFiles, setFormFiles] = useState<AttachFile[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Edit modal
  const [editOpen, setEditOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<Defect | null>(null);

  // Delete modal
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null);

  // Inline severity / status quick-update
  const handleSeverityChange = (defectId: string, newSeverity: string) => {
    updateDefect.mutate(
      { projectId: projectId!, id: defectId, body: { severity: newSeverity as Defect['severity'] } },
      {
        onSuccess: () => toast.success(`Severity updated to ${newSeverity}`),
        onError:   (e) => toast.error(e.message),
      }
    );
  };

  const handleStatusChange = (defectId: string, newStatus: string) => {
    updateDefect.mutate(
      { projectId: projectId!, id: defectId, body: { status: newStatus as Defect['status'] } },
      {
        onSuccess: () => toast.success(`Status updated to ${newStatus}`),
        onError:   (e) => toast.error(e.message),
      }
    );
  };

  // View modal
  const [viewOpen, setViewOpen] = useState(false);
  const [viewTarget, setViewTarget] = useState<Defect | null>(null);

  // Build a stable display-ID map (DEF-01, DEF-02…) ordered by creation time
  const defectDisplayIdMap = useMemo(() => {
    const sorted = [...defects].sort((a, b) => a.createdAt.localeCompare(b.createdAt));
    const map: Record<string, string> = {};
    sorted.forEach((d, i) => { map[d.id] = listDisplayId('DEF', i); });
    return map;
  }, [defects]);

  const openCount = defects.filter((d) => d.status === 'Open').length;

  const filtered = defects
    .filter((d) => {
      const matchSearch =
        d.title.toLowerCase().includes(search.toLowerCase()) ||
        d.id.toLowerCase().includes(search.toLowerCase());
      const matchStatus = statusFilter === 'all' || d.status === statusFilter;
      return matchSearch && matchStatus;
    })
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt));

  const resetForm = () => {
    setFormTitle('');
    setFormDesc('');
    setFormSeverity('Major');
    setFormStatus('Open');
    setFormLink('');
    setFormSource('');
    setFormFiles([]);
  };

  const handleFileUpload = (fileList: FileList | null) => {
    if (!fileList) return;
    const newFiles: AttachFile[] = Array.from(fileList).map((f) => ({ name: f.name, size: f.size, type: f.type }));
    setFormFiles((prev) => [...prev, ...newFiles]);
  };

  const handleCreate = () => {
    if (!formTitle.trim()) {
      toast.error('Title is required');
      return;
    }
    createDefect.mutate(
      {
        projectId: projectId!,
        body: {
          title:       formTitle,
          description: formDesc,
          severity:    formSeverity,
          link:        formLink,
          status:      formStatus,
          source:      formSource,
        },
      },
      {
        onSuccess: () => {
          toast.success('Defect created');
          resetForm();
          setCreateOpen(false);
        },
        onError: (e) => toast.error(e.message),
      }
    );
  };

  const openEdit = (d: Defect) => {
    setEditTarget({ ...d });
    setEditOpen(true);
  };

  const handleEdit = () => {
    if (!editTarget) return;
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
      {
        onSuccess: () => {
          toast.success('Defect updated');
          setEditOpen(false);
          setEditTarget(null);
        },
        onError: (e) => toast.error(e.message),
      }
    );
  };

  const handleDelete = () => {
    if (!deleteTarget) return;
    deleteDefect.mutate(
      { projectId: projectId!, id: deleteTarget },
      {
        onSuccess: () => {
          toast.success('Defect deleted');
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
                { label: project?.name || 'Project', href: `/projects/${projectId}/repository` },
                { label: 'Defects' },
              ]} />
            </div>
            <div className="flex items-center justify-between">
              <div>
                <h1 className="text-xl font-bold text-foreground tracking-[-0.02em]">Defects</h1>
                <p className="text-xs text-muted-foreground mt-0.5">
                  {openCount} open · {defects.length} total
                </p>
              </div>
              <Button
                size="sm"
                className="bg-gradient-to-br from-primary to-primary/80 text-primary-foreground gap-1.5 border-0"
                onClick={() => setCreateOpen(true)}
              >
                <Plus className="h-3.5 w-3.5" strokeWidth={1.5} /> Create Defect
              </Button>
            </div>
          </div>

          {/* Filters */}
          <div className="flex items-center gap-3 mb-4">
            <div className="relative flex-1 max-w-xs">
              <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground" />
              <Input
                placeholder="Search defects..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="pl-8 h-8 text-xs bg-secondary border-0"
              />
            </div>
            <Select value={statusFilter} onValueChange={setStatusFilter}>
              <SelectTrigger className="w-36 h-8 text-xs bg-secondary border-0">
                <SelectValue placeholder="All statuses" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Statuses</SelectItem>
                <SelectItem value="Open">Open</SelectItem>
                <SelectItem value="In Progress">In Progress</SelectItem>
                <SelectItem value="Fixed">Fixed</SelectItem>
                <SelectItem value="Closed">Closed</SelectItem>
              </SelectContent>
            </Select>
          </div>

          {/* Loading */}
          {isLoading && (
            <div className="flex items-center justify-center py-20 gap-2 text-muted-foreground">
              <Loader2 className="h-5 w-5 animate-spin" />
              <span className="text-sm">Loading defects…</span>
            </div>
          )}

          {/* Error */}
          {isError && (
            <div className="rounded-lg border border-destructive/30 bg-destructive/10 text-destructive px-5 py-4 text-sm flex items-center gap-2">
              <AlertTriangle className="h-4 w-4 shrink-0" strokeWidth={1.5} />
              {(error as Error)?.message ?? 'Failed to load defects.'}
            </div>
          )}

          {/* Table */}
          {!isLoading && !isError && (
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
                {filtered.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="text-center py-12 text-muted-foreground text-sm">No defects found</td>
                  </tr>
                ) : (
                  filtered.map((d) => (
                    <tr
                      key={d.id}
                      className="hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-colors border-b border-slate-100 dark:border-slate-700/50 bg-card cursor-pointer"
                      onClick={() => { setViewTarget(d); setViewOpen(true); }}
                    >
                      <td className="px-5 py-3.5 font-mono text-[10px] text-accent tracking-wider" title={d.id}>{defectDisplayIdMap[d.id] ?? '-'}</td>
                      <td className="px-5 py-3.5 font-medium text-foreground">{d.title}</td>
                      <td className="px-5 py-3.5" onClick={(e) => e.stopPropagation()}>
                        <Select value={d.severity} onValueChange={(val) => handleSeverityChange(d.id, val)}>
                          <SelectTrigger className={`h-7 w-auto text-[10px] font-mono uppercase tracking-widest rounded-md px-2.5 ${severityBadge[d.severity] || ''}`}>
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
                            onClick={() => { setDeleteTarget(d.id); setDeleteOpen(true); }}
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

      {/* Create Defect Modal */}
      <Dialog open={createOpen} onOpenChange={(v) => { if (!v) resetForm(); setCreateOpen(v); }}>
        <DialogContent className="sm:max-w-3xl bg-card">
          <DialogHeader>
            <DialogTitle className="text-foreground">Create Defect</DialogTitle>
            <DialogDescription className="text-muted-foreground">Report a new issue.</DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div>
              <label className={labelCls}>Title</label>
              <Input value={formTitle} onChange={(e) => setFormTitle(e.target.value)} placeholder="Brief summary..." className="mt-0 h-9 bg-white border border-input focus-visible:ring-1 focus-visible:ring-ring" />
            </div>
            <div>
              <label className={labelCls}>Description</label>
              <Textarea value={formDesc} onChange={(e) => setFormDesc(e.target.value)} placeholder="Steps to reproduce..." className="mt-0 min-h-[140px] bg-white border border-input focus-visible:ring-1 focus-visible:ring-ring text-sm resize-none" />
            </div>
            <div className="grid grid-cols-3 gap-3">
              <div>
                <label className={labelCls}>Severity</label>
                <Select value={formSeverity} onValueChange={(v) => setFormSeverity(v as 'Critical' | 'Major' | 'Minor')}>
                  <SelectTrigger className="mt-0 h-9 bg-white border border-input"><SelectValue /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="Critical">Critical</SelectItem>
                    <SelectItem value="Major">Major</SelectItem>
                    <SelectItem value="Minor">Minor</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div>
                <label className={labelCls}>Status</label>
                <Select value={formStatus} onValueChange={(v) => setFormStatus(v as 'Open' | 'In Progress' | 'Fixed' | 'Closed')}>
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
                <label className={labelCls}>Source</label>
                <Input value={formSource} onChange={(e) => setFormSource(e.target.value)} placeholder="TR-01 or AS-1" className="mt-0 h-9 bg-white border border-input focus-visible:ring-1 focus-visible:ring-ring" />
              </div>
            </div>
            <div>
              <label className={labelCls}>External Link</label>
              <Input value={formLink} onChange={(e) => setFormLink(e.target.value)} placeholder="https://jira.example.com/..." className="mt-0 h-9 bg-white border border-input focus-visible:ring-1 focus-visible:ring-ring" />
            </div>
            {/* Attachments */}
            <div>
              <label className={labelCls}>Attachments</label>
              <div
                className="mt-0 border-2 border-dashed border-border rounded-lg p-3 text-center cursor-pointer hover:border-primary/40 hover:bg-primary/5 transition-colors"
                onClick={() => fileInputRef.current?.click()}
                onDragOver={(e) => { e.preventDefault(); e.stopPropagation(); }}
                onDrop={(e) => { e.preventDefault(); e.stopPropagation(); handleFileUpload(e.dataTransfer.files); }}
              >
                <Upload className="h-5 w-5 text-muted-foreground mx-auto mb-1" strokeWidth={1.5} />
                <p className="text-xs text-foreground font-medium">Drop files or click to browse</p>
                <p className="text-[10px] text-muted-foreground mt-0.5">Screenshots, logs, PDFs</p>
                <input ref={fileInputRef} type="file" multiple className="hidden" onChange={(e) => handleFileUpload(e.target.files)} />
              </div>
              {formFiles.length > 0 && (
                <div className="mt-2 space-y-1.5">
                  {formFiles.map((file, i) => {
                    const IconComp = getFileIcon(file.type);
                    return (
                      <div key={i} className="flex items-center gap-2.5 px-3 py-2 bg-secondary rounded-md">
                        <IconComp className="h-4 w-4 text-muted-foreground shrink-0" strokeWidth={1.5} />
                        <span className="text-sm text-foreground truncate flex-1">{file.name}</span>
                        <span className="text-xs text-muted-foreground shrink-0">{formatFileSize(file.size)}</span>
                        <button onClick={() => setFormFiles((prev) => prev.filter((_, j) => j !== i))} className="text-muted-foreground hover:text-destructive transition-colors">
                          <X className="h-3.5 w-3.5" strokeWidth={1.5} />
                        </button>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </div>
          <DialogFooter className="mt-4">
            <Button variant="ghost" onClick={() => { resetForm(); setCreateOpen(false); }}>Cancel</Button>
            <Button onClick={handleCreate}>Create Defect</Button>
          </DialogFooter>
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
                <label className={labelCls}>Title</label>
                <Input value={editTarget.title} onChange={(e) => setEditTarget({ ...editTarget, title: e.target.value })} className="mt-0 h-9 bg-white border border-input focus-visible:ring-1 focus-visible:ring-ring" />
              </div>
              <div>
                <label className={labelCls}>Description</label>
                <Textarea value={editTarget.description} onChange={(e) => setEditTarget({ ...editTarget, description: e.target.value })} className="mt-0 min-h-[140px] bg-white border border-input focus-visible:ring-1 focus-visible:ring-ring text-sm resize-none" />
              </div>
              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className={labelCls}>Severity</label>
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
                  <label className={labelCls}>Status</label>
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
                  <label className={labelCls}>Source</label>
                  <Input value={editTarget.source || ''} onChange={(e) => setEditTarget({ ...editTarget, source: e.target.value })} placeholder="TR-01 or AS-1" className="mt-0 h-9 bg-white border border-input focus-visible:ring-1 focus-visible:ring-ring" />
                </div>
              </div>
              <div>
                <label className={labelCls}>External Link</label>
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
                <label className={labelCls}>Description</label>
                <div className="mt-0 px-3 py-2 bg-white border border-input rounded-md text-sm text-foreground min-h-[140px] resize-none leading-relaxed overflow-y-auto max-h-[200px]">
                  {viewTarget.description || 'No description provided.'}
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className={labelCls}>Source</label>
                  <div className="mt-0 h-9 px-3 bg-white border border-input rounded-md text-sm text-foreground font-mono flex items-center">
                    {viewTarget.source ? (isUuid(viewTarget.source) ? viewTarget.source.slice(0, 8) : viewTarget.source) : '—'}
                  </div>
                </div>
                <div>
                  <label className={labelCls}>Created</label>
                  <div className="mt-0 h-9 px-3 bg-white border border-input rounded-md text-sm text-foreground flex items-center">
                    {formatDate(viewTarget.createdAt)}
                  </div>
                </div>
              </div>
              {viewTarget.link && (
                <div>
                  <label className={labelCls}>External Link</label>
                  <a href={viewTarget.link} target="_blank" rel="noopener noreferrer" className="mt-0 h-9 px-3 bg-white border border-input rounded-md text-sm text-purple-600 hover:text-purple-700 flex items-center gap-1 hover:underline">
                    {viewTarget.link} <ExternalLink className="h-3 w-3" />
                  </a>
                </div>
              )}
              <div>
                <label className={labelCls}>Attachments</label>
                <p className="text-sm text-muted-foreground mt-1.5">Attachments are managed on the Attachments page.</p>
              </div>
            </div>
          )}
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation */}
      <Dialog open={deleteOpen} onOpenChange={setDeleteOpen}>
        <DialogContent className="sm:max-w-sm bg-card">
          <DialogHeader>
            <DialogTitle className="text-foreground flex items-center gap-3">
              <AlertTriangle className="h-6 w-6 text-destructive shrink-0" strokeWidth={1.5} />
              Remove Defect
            </DialogTitle>
            <DialogDescription className="text-sm text-foreground mt-3">
              Are you sure you want to delete <span className="font-bold">{deleteTarget}</span>? This action cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter className="mt-6">
            <Button variant="outline" onClick={() => setDeleteOpen(false)}>Cancel</Button>
            <Button variant="destructive" onClick={handleDelete}>Remove</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default Defects;
