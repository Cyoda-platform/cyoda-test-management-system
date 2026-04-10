import { AlertTriangle } from 'lucide-react';
import {
  Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';

interface DeleteProjectModalProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  projectName: string;
}

const DeleteProjectModal = ({ open, onClose, onConfirm, projectName }: DeleteProjectModalProps) => {
  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-sm bg-card">
        <DialogHeader>
          <DialogTitle className="text-foreground flex items-center gap-3">
            <AlertTriangle className="h-6 w-6 text-destructive shrink-0" strokeWidth={1.5} />
            Delete Project
          </DialogTitle>
          <DialogDescription className="text-sm text-foreground mt-3">
            Are you sure you want to delete <span className="font-bold">{projectName}</span>? This action cannot be undone.
          </DialogDescription>
        </DialogHeader>
        <DialogFooter className="mt-6">
          <Button variant="outline" onClick={onClose}>Cancel</Button>
          <Button variant="destructive" onClick={onConfirm}>Delete</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

export default DeleteProjectModal;
