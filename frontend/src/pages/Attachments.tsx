import { useState } from 'react';
import { Search, Grid, List, FileText, Image, File, Download, Trash2, X, ChevronRight, AlertTriangle } from 'lucide-react';
import Breadcrumbs from '@/components/Breadcrumbs';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { useParams, Link } from 'react-router-dom';
import { mockProjects } from '@/data/mockData';
import {
  Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription,
} from '@/components/ui/dialog';

const mockFiles = [
  { id: '1', name: 'login-error-screenshot.png', size: '2.4 MB', source: 'TR-01 > Step 3', sourceLink: '#', uploader: 'admin', date: '2026-03-20', type: 'image', url: '/placeholder.svg' },
  { id: '2', name: 'api-response-log.txt', size: '48 KB', source: 'TR-02 > Step 1', sourceLink: '#', uploader: 'qa_lead', date: '2026-03-19', type: 'log', url: '/placeholder.svg' },
  { id: '3', name: 'test-data-payload.json', size: '12 KB', source: 'Manual Upload', sourceLink: '#', uploader: 'admin', date: '2026-03-18', type: 'document', url: '/placeholder.svg' },
  { id: '4', name: 'dashboard-regression.png', size: '1.8 MB', source: 'TR-04 > Step 5', sourceLink: '#', uploader: 'tester01', date: '2026-03-17', type: 'image', url: '/placeholder.svg' },
];

const fileIcons: Record<string, typeof FileText> = { image: Image, log: FileText, document: File };

const Attachments = () => {
  const { projectId } = useParams();
  const project = mockProjects.find((p) => p.id === projectId);
  const [viewMode, setViewMode] = useState<'table' | 'grid'>('table');
  const [search, setSearch] = useState('');
  const [previewFile, setPreviewFile] = useState<typeof mockFiles[0] | null>(null);
  const [deleteFile, setDeleteFile] = useState<typeof mockFiles[0] | null>(null);
  const [files, setFiles] = useState(mockFiles);

  const filtered = files
    .filter(f => f.name.toLowerCase().includes(search.toLowerCase()))
    .sort((a, b) => b.date.localeCompare(a.date));

  const handleFileClick = (e: React.MouseEvent, file: typeof mockFiles[0]) => {
    if (e.button === 0) {
      e.preventDefault();
      setPreviewFile(file);
    }
  };

  const handleDelete = () => {
    if (deleteFile) {
      setFiles(prev => prev.filter(f => f.id !== deleteFile.id));
      setDeleteFile(null);
    }
  };

  const handleDownload = (file: typeof mockFiles[0]) => {
    const a = document.createElement('a');
    a.href = file.url;
    a.download = file.name;
    a.click();
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
              <Button
                variant={viewMode === 'grid' ? 'secondary' : 'ghost'}
                size="icon"
                className="h-8 w-8"
                onClick={() => setViewMode('grid')}
              >
                <Grid className="h-4 w-4" strokeWidth={1.5} />
              </Button>
              <Button
                variant={viewMode === 'table' ? 'secondary' : 'ghost'}
                size="icon"
                className="h-8 w-8"
                onClick={() => setViewMode('table')}
              >
                <List className="h-4 w-4" strokeWidth={1.5} />
              </Button>
            </div>
          </div>

          {/* Content */}
          {viewMode === 'table' ? (
          <div className="bg-card rounded-lg shadow-soft overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-slate-200 dark:bg-slate-700 sticky top-0 z-10">
                <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">File Name</th>
                <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Size</th>
                <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Source</th>
                <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Uploader</th>
                <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Date</th>
                <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((f) => {
                const Icon = fileIcons[f.type] || File;
                return (
                  <tr key={f.id} className="hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-colors border-b border-slate-100 dark:border-slate-700/50">
                    <td className="px-5 py-3.5">
                      <a
                        href={f.url}
                        target="_blank"
                        rel="noopener noreferrer"
                        onClick={(e) => handleFileClick(e, f)}
                        className="flex items-center gap-2 cursor-pointer"
                      >
                        <Icon className="h-4 w-4 text-muted-foreground" strokeWidth={1.5} />
                        <span className="font-medium text-foreground hover:text-primary transition-colors">{f.name}</span>
                      </a>
                    </td>
                    <td className="px-5 py-3.5 text-muted-foreground font-mono text-xs">{f.size}</td>
                    <td className="px-5 py-3.5">
                      <Link to={f.sourceLink} className="text-primary text-[10px] hover:underline font-mono tracking-wider flex items-center gap-0.5">
                        {f.source.split(' > ').map((s, i, arr) => (
                          <span key={i} className="flex items-center gap-0.5">
                            {s}{i < arr.length - 1 && <ChevronRight className="h-3 w-3 text-muted-foreground" />}
                          </span>
                        ))}
                      </Link>
                    </td>
                    <td className="px-5 py-3.5 text-muted-foreground">{f.uploader}</td>
                    <td className="px-5 py-3.5 text-muted-foreground font-mono text-[10px] tracking-wider">{f.date}</td>
                    <td className="px-5 py-3.5">
                      <div className="flex items-center gap-1">
                        <Button variant="ghost" size="icon" className="h-7 w-7" onClick={() => handleDownload(f)}>
                          <Download className="h-3.5 w-3.5 text-muted-foreground" strokeWidth={1.5} />
                        </Button>
                        <Button variant="ghost" size="icon" className="h-7 w-7" onClick={() => setDeleteFile(f)}>
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
          ) : (
          /* Grid View */
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
            {filtered.map((f) => {
              const Icon = fileIcons[f.type] || File;
              const isImage = f.type === 'image';
              return (
                <div key={f.id} className="rounded-lg border border-border bg-card overflow-hidden group">
                  <a
                    href={f.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    onClick={(e) => handleFileClick(e, f)}
                    className="block cursor-pointer"
                  >
                    <div className="aspect-[4/3] bg-muted/30 flex items-center justify-center overflow-hidden">
                      {isImage ? (
                        <img src={f.url} alt={f.name} className="w-full h-full object-cover" />
                      ) : (
                        <Icon className="h-12 w-12 text-muted-foreground/40" strokeWidth={1} />
                      )}
                    </div>
                  </a>
                  <div className="p-3 space-y-1">
                    <a
                      href={f.url}
                      target="_blank"
                      rel="noopener noreferrer"
                      onClick={(e) => handleFileClick(e, f)}
                      className="block cursor-pointer"
                    >
                      <p className="text-sm font-medium text-foreground truncate hover:text-primary transition-colors">{f.name}</p>
                    </a>
                    <p className="text-[10px] font-mono text-muted-foreground tracking-wider">{f.source}</p>
                    <div className="flex items-center justify-between pt-1">
                      <span className="text-xs text-muted-foreground">{f.size}</span>
                      <div className="flex items-center gap-0.5">
                        <Button variant="ghost" size="icon" className="h-6 w-6" onClick={() => handleDownload(f)}>
                          <Download className="h-3 w-3 text-muted-foreground" strokeWidth={1.5} />
                        </Button>
                        <Button variant="ghost" size="icon" className="h-6 w-6" onClick={() => setDeleteFile(f)}>
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
            <DialogTitle className="text-base font-semibold">{previewFile?.name}</DialogTitle>
            <DialogDescription className="text-xs text-muted-foreground">
              {previewFile?.source} · {previewFile?.size}
            </DialogDescription>
          </DialogHeader>
          <div className="flex items-center justify-center min-h-[300px] bg-muted/20 rounded-lg overflow-hidden">
            {previewFile?.type === 'image' ? (
              <img src={previewFile.url} alt={previewFile.name} className="max-w-full max-h-[60vh] object-contain" />
            ) : (
              <div className="flex flex-col items-center gap-3 text-muted-foreground">
                <FileText className="h-16 w-16" strokeWidth={1} />
                <p className="text-sm">Preview not available for this file type</p>
              </div>
            )}
          </div>
          <DialogFooter>
            <Button variant="ghost" onClick={() => setPreviewFile(null)}>Close</Button>
            <Button onClick={() => previewFile && handleDownload(previewFile)} className="gap-1.5">
              <Download className="h-3.5 w-3.5" /> Download
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation Modal */}
      <Dialog open={!!deleteFile} onOpenChange={() => setDeleteFile(null)}>
        <DialogContent className="sm:max-w-sm bg-card">
          <DialogHeader>
            <DialogTitle className="text-foreground flex items-center gap-3">
              <AlertTriangle className="h-6 w-6 text-destructive shrink-0" strokeWidth={1.5} />
              Delete Attachment
            </DialogTitle>
            <DialogDescription className="text-sm text-foreground mt-3">
              Are you sure you want to delete <span className="font-bold">{deleteFile?.name}</span>? This action cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter className="mt-6">
            <Button variant="outline" onClick={() => setDeleteFile(null)}>Cancel</Button>
            <Button variant="destructive" onClick={handleDelete}>Delete</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default Attachments;
