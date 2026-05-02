import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { ExternalLink } from 'lucide-react';
import DefectAttachmentsUpload from './DefectAttachmentsUpload';

const labelCls = 'text-[10px] font-semibold text-muted-foreground uppercase mb-1.5 block font-mono tracking-widest';

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

interface Defect {
  id: string;
  title: string;
  description: string;
  severity: 'Critical' | 'Major' | 'Minor';
  status: 'Open' | 'In Progress' | 'Fixed' | 'Closed';
  source?: string;
  link: string;
  createdAt?: string;
}

interface AttachmentFile {
  id?: string;
  file?: File;
  name: string;
  size: number;
  type: string;
}

interface ViewDefectModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  defect: Defect | null;
  displayId?: string;
  existingAttachments?: AttachmentFile[];
  formatDate?: (date: string) => string;
  projectId?: string;
}

const ViewDefectModal = ({
  open,
  onOpenChange,
  defect,
  displayId,
  existingAttachments = [],
  formatDate = (d) => d,
  projectId,
}: ViewDefectModalProps) => {
  if (!defect) return null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-3xl bg-card">
        <DialogHeader>
          <DialogTitle className="text-foreground">View Defect</DialogTitle>
          <DialogDescription className="text-muted-foreground font-mono">{displayId || '-'}</DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          {/* Title */}
          <div>
            <label className={labelCls}>Title</label>
            <div className="mt-0 h-9 px-3 py-2 rounded-md bg-white border border-input flex items-center">
              <p className="text-foreground text-sm">{defect.title}</p>
            </div>
          </div>

          {/* Description */}
          <div>
            <label className={labelCls}>Description</label>
            <div className="mt-0 min-h-[140px] p-3 rounded-md bg-white border border-input">
              <p className="text-foreground text-sm whitespace-pre-wrap">{defect.description}</p>
            </div>
          </div>

          {/* Severity, Status, Source (3-column) */}
          <div className="grid grid-cols-3 gap-3">
            <div>
              <label className={labelCls}>Severity</label>
              <div className="mt-0 h-9 px-3 py-2 rounded-md bg-white border border-input flex items-center">
                <span className={`text-sm font-medium ${severityBadge[defect.severity] || 'text-foreground'}`}>
                  {defect.severity}
                </span>
              </div>
            </div>
            <div>
              <label className={labelCls}>Status</label>
              <div className="mt-0 h-9 px-3 py-2 rounded-md bg-white border border-input flex items-center">
                <span className={`text-sm font-medium ${statusBadge[defect.status] || 'text-foreground'}`}>
                  {defect.status}
                </span>
              </div>
            </div>
            <div>
              <label className={labelCls}>Source</label>
              <div className="mt-0 h-9 px-3 py-2 rounded-md bg-white border border-input flex items-center">
                <p className="text-foreground text-sm">{defect.source || '-'}</p>
              </div>
            </div>
          </div>

          {/* External Link */}
          <div>
            <label className={labelCls}>External Link</label>
            <div className="mt-0 min-h-9 px-3 py-2 rounded-md bg-white border border-input flex items-start gap-2">
              {defect.link ? (
                <a
                  href={defect.link}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-blue-600 hover:text-blue-800 underline text-sm inline-flex items-center gap-1 break-all"
                >
                  {defect.link}
                  <ExternalLink className="h-3 w-3 shrink-0" />
                </a>
              ) : (
                <p className="text-muted-foreground text-sm">-</p>
              )}
            </div>
          </div>

          {/* Attachments (Read-only) */}
          <div>
            <label className={labelCls}>Attachments</label>
            <DefectAttachmentsUpload files={existingAttachments} onFilesChange={() => {}} readonly={true} projectId={projectId} />
          </div>
        </div>

        <DialogFooter className="mt-4">
          <Button variant="ghost" onClick={() => onOpenChange(false)}>
            Close
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

export default ViewDefectModal;
