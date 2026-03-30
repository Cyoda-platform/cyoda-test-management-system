import { useState, useRef, useEffect } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Upload, FileText, Image, File, X } from 'lucide-react';
import { toast } from 'sonner';

const labelCls = 'text-[10px] font-semibold text-muted-foreground uppercase mb-1.5 block font-mono tracking-widest';

interface EvidenceFile {
  name: string;
  size: number;
  type: string;
}

interface DefectData {
  title: string;
  description: string;
  severity: 'Critical' | 'Major' | 'Minor';
  status: 'Open' | 'In Progress' | 'Fixed' | 'Closed';
  source: string;
  link: string;
  files: EvidenceFile[];
  caseId: string;
  stepIdx?: number;
}

interface CreateDefectModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  caseId: string;
  caseTitle: string;
  stepIdx?: number;
  sourceInitial?: string;
  onCreateDefect: (defect: DefectData) => void;
}

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

const CreateDefectModal = ({ open, onOpenChange, caseId, caseTitle, stepIdx, sourceInitial, onCreateDefect }: CreateDefectModalProps) => {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [severity, setSeverity] = useState<'Critical' | 'Major' | 'Minor'>('Major');
  const [status, setStatus] = useState<'Open' | 'In Progress' | 'Fixed' | 'Closed'>('Open');
  const [source, setSource] = useState(sourceInitial || '');
  const [link, setLink] = useState('');
  const [files, setFiles] = useState<EvidenceFile[]>([]);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (sourceInitial) {
      setSource(sourceInitial);
    }
  }, [sourceInitial, open]);

  const contextLabel = stepIdx !== undefined
    ? `Step ${stepIdx + 1} of ${caseId}`
    : `Case: ${caseTitle}`;

  const handleFileUpload = (fileList: FileList | null) => {
    if (!fileList) return;
    const newFiles: EvidenceFile[] = Array.from(fileList).map((f) => ({
      name: f.name,
      size: f.size,
      type: f.type,
    }));
    setFiles((prev) => [...prev, ...newFiles]);
  };

  const removeFile = (idx: number) => {
    setFiles((prev) => prev.filter((_, i) => i !== idx));
  };

  const handleSubmit = () => {
    if (!title.trim()) {
      toast.error('Defect title is required');
      return;
    }
    onCreateDefect({ title, description, severity, status, source, link, files, caseId, stepIdx });
    toast.success('Defect created successfully');
    setTitle('');
    setDescription('');
    setSeverity('Major');
    setStatus('Open');
    setSource('');
    setLink('');
    setFiles([]);
    onOpenChange(false);
  };

  const handleClose = (val: boolean) => {
    if (!val) {
      setTitle('');
      setDescription('');
      setSeverity('Major');
      setStatus('Open');
      setSource('');
      setLink('');
      setFiles([]);
    }
    onOpenChange(val);
  };

  return (
    <Dialog open={open} onOpenChange={handleClose}>
      <DialogContent className="sm:max-w-3xl bg-card">
        <DialogHeader>
          <DialogTitle className="text-foreground">Create Defect</DialogTitle>
          <DialogDescription className="text-muted-foreground">
            Report an issue for {contextLabel}.
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          {/* Title */}
          <div>
            <label className={labelCls}>Defect Title</label>
            <Input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Brief summary of the issue..."
              className="mt-0 h-9 bg-white border border-input focus-visible:ring-1 focus-visible:ring-ring"
            />
          </div>

          {/* Description */}
          <div>
            <label className={labelCls}>Description</label>
            <Textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Steps to reproduce, observed vs expected behavior..."
              className="mt-0 min-h-[140px] bg-white border border-input focus-visible:ring-1 focus-visible:ring-ring text-sm resize-none"
            />
          </div>

          {/* Severity & Status & Link */}
          <div className="grid grid-cols-3 gap-3">
            <div>
              <label className={labelCls}>Severity</label>
              <Select value={severity} onValueChange={(v) => setSeverity(v as 'Critical' | 'Major' | 'Minor')}>
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
              <Select value={status} onValueChange={(v) => setStatus(v as 'Open' | 'In Progress' | 'Fixed' | 'Closed')}>
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
              <Input
                value={source}
                onChange={(e) => setSource(e.target.value)}
                placeholder="TR-01 or AS-1"
                className="mt-0 h-9 bg-white border border-input focus-visible:ring-1 focus-visible:ring-ring"
              />
            </div>
          </div>

          {/* External Link */}
          <div>
            <label className={labelCls}>External Link</label>
            <Input
              value={link}
              onChange={(e) => setLink(e.target.value)}
              placeholder="https://jira.example.com/BUG-123"
              className="mt-0 h-9 bg-white border border-input focus-visible:ring-1 focus-visible:ring-ring"
            />
          </div>

          {/* File Upload */}
          <div>
            <label className={labelCls}>Attachments</label>
            <div
              className="mt-1.5 border-2 border-dashed border-border rounded-lg p-3 text-center cursor-pointer hover:border-primary/40 hover:bg-primary/5 transition-colors"
              onClick={() => fileInputRef.current?.click()}
              onDragOver={(e) => { e.preventDefault(); e.stopPropagation(); }}
              onDrop={(e) => {
                e.preventDefault();
                e.stopPropagation();
                handleFileUpload(e.dataTransfer.files);
              }}
            >
              <Upload className="h-5 w-5 text-muted-foreground mx-auto mb-1" strokeWidth={1.5} />
              <p className="text-xs text-foreground font-medium">Drop files or click to browse</p>
              <p className="text-[10px] text-muted-foreground mt-0.5">Screenshots, logs, PDFs</p>
              <input
                ref={fileInputRef}
                type="file"
                multiple
                className="hidden"
                onChange={(e) => handleFileUpload(e.target.files)}
              />
            </div>
          </div>

          {/* Uploaded Files */}
          {files.length > 0 && (
            <div className="space-y-1.5">
              {files.map((file, i) => {
                const IconComp = getFileIcon(file.type);
                return (
                  <div key={i} className="flex items-center gap-2.5 px-3 py-2 bg-secondary rounded-md">
                    <IconComp className="h-4 w-4 text-muted-foreground shrink-0" strokeWidth={1.5} />
                    <span className="text-sm text-foreground truncate flex-1">{file.name}</span>
                    <span className="text-xs text-muted-foreground shrink-0">{formatFileSize(file.size)}</span>
                    <button onClick={() => removeFile(i)} className="text-muted-foreground hover:text-destructive transition-colors">
                      <X className="h-3.5 w-3.5" strokeWidth={1.5} />
                    </button>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        <div className="flex justify-end gap-2 mt-4">
          <Button variant="ghost" onClick={() => handleClose(false)}>Cancel</Button>
          <Button
            onClick={handleSubmit}
            className="bg-primary text-primary-foreground hover:bg-primary/90"
          >
            Create Defect
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
};

export default CreateDefectModal;
