import { useState, useEffect } from 'react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import DefectAttachmentsUpload from './DefectAttachmentsUpload';
import { toast } from 'sonner';

const labelCls = 'text-[10px] font-semibold text-muted-foreground uppercase mb-1.5 block font-mono tracking-widest';

interface Defect {
  id: string;
  title: string;
  description: string;
  severity: 'Critical' | 'Major' | 'Minor';
  status: 'Open' | 'In Progress' | 'Fixed' | 'Closed';
  source?: string;
  link: string;
}

interface AttachmentFile {
  id?: string;
  file?: File;
  name: string;
  size: number;
  type: string;
}

interface EditDefectModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  defect: Defect | null;
  displayId?: string;
  existingAttachments?: AttachmentFile[];
  onSave: (defect: Defect, newFiles: File[], removedAttachmentIds: string[]) => Promise<void>;
  projectId?: string;
}

const EditDefectModal = ({
  open,
  onOpenChange,
  defect,
  displayId,
  existingAttachments = [],
  onSave,
  projectId,
}: EditDefectModalProps) => {
  const [editData, setEditData] = useState<Defect | null>(null);
  const [attachments, setAttachments] = useState<AttachmentFile[]>([]);
  const [removedAttachmentIds, setRemovedAttachmentIds] = useState<string[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (open && defect) {
      setEditData({ ...defect });
      setAttachments(existingAttachments);
      setRemovedAttachmentIds([]);
    }
  }, [open, defect, existingAttachments]);

  const handleSave = async () => {
    if (!editData || !editData.title.trim()) {
      toast.error('Defect title is required');
      return;
    }
    if (isSubmitting) return;

    setIsSubmitting(true);
    try {
      // Get new files (those with file property)
      const newFiles = attachments
        .filter((att) => att.file)
        .map((att) => att.file!);

      await onSave(editData, newFiles, removedAttachmentIds);
      onOpenChange(false);
    } catch (error) {
      // Error toast is shown by caller
    } finally {
      setIsSubmitting(false);
    }
  };

  if (!editData) return null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-3xl bg-card">
        <DialogHeader>
          <DialogTitle className="text-foreground">Edit Defect</DialogTitle>
          <DialogDescription className="text-muted-foreground font-mono">{displayId || '-'}</DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          {/* Title */}
          <div>
            <label className={labelCls}>Title</label>
            <Input
              value={editData.title}
              onChange={(e) => setEditData({ ...editData, title: e.target.value })}
              className="mt-0 h-9 bg-white border border-input focus-visible:ring-1 focus-visible:ring-ring"
            />
          </div>

          {/* Description */}
          <div>
            <label className={labelCls}>Description</label>
            <Textarea
              value={editData.description}
              onChange={(e) => setEditData({ ...editData, description: e.target.value })}
              className="mt-0 min-h-[140px] bg-white border border-input focus-visible:ring-1 focus-visible:ring-ring text-sm resize-none"
            />
          </div>

          {/* Severity, Status, Source */}
          <div className="grid grid-cols-3 gap-3">
            <div>
              <label className={labelCls}>Severity</label>
              <Select
                value={editData.severity}
                onValueChange={(v) => setEditData({ ...editData, severity: v as 'Critical' | 'Major' | 'Minor' })}
              >
                <SelectTrigger className="mt-0 h-9 bg-white border border-input">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="Critical">Critical</SelectItem>
                  <SelectItem value="Major">Major</SelectItem>
                  <SelectItem value="Minor">Minor</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div>
              <label className={labelCls}>Status</label>
              <Select
                value={editData.status}
                onValueChange={(v) => setEditData({ ...editData, status: v as Defect['status'] })}
              >
                <SelectTrigger className="mt-0 h-9 bg-white border border-input">
                  <SelectValue />
                </SelectTrigger>
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
                value={editData.source || ''}
                onChange={(e) => setEditData({ ...editData, source: e.target.value })}
                placeholder="TR-01 or AS-1"
                className="mt-0 h-9 bg-white border border-input focus-visible:ring-1 focus-visible:ring-ring"
              />
            </div>
          </div>

          {/* External Link */}
          <div>
            <label className={labelCls}>External Link</label>
            <Input
              value={editData.link}
              onChange={(e) => setEditData({ ...editData, link: e.target.value })}
              placeholder="https://jira.example.com/BUG-123"
              className="mt-0 h-9 bg-white border border-input focus-visible:ring-1 focus-visible:ring-ring"
            />
          </div>

          {/* Attachments */}
          <div>
            <label className={labelCls}>Attachments</label>
            <DefectAttachmentsUpload
              files={attachments}
              projectId={projectId}
              onFilesChange={(newFiles) => {
                // Track removed attachments
                const newIds = new Set(newFiles.map((f) => f.id).filter((id) => id));
                const oldIds = new Set(attachments.map((f) => f.id).filter((id) => id));
                const removed = Array.from(oldIds).filter((id) => !newIds.has(id));
                setRemovedAttachmentIds((prev) => [...new Set([...prev, ...removed])]);
                setAttachments(newFiles);
              }}
              readonly={false}
            />
          </div>
        </div>

        <DialogFooter className="mt-4">
          <Button variant="ghost" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button onClick={handleSave} disabled={isSubmitting}>
            {isSubmitting ? 'Saving...' : 'Save Changes'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

export default EditDefectModal;
