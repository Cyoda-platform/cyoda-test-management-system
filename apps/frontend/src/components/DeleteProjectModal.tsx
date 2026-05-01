import { AlertTriangle, Loader2 } from 'lucide-react';
import {
  Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';

interface DeleteProjectModalProps {
  open: boolean;
  onClose: () => void;
  onConfirm: () => void;
  projectName: string;
  isLoading?: boolean;
}

const DeleteProjectModal = ({ open, onClose, onConfirm, projectName, isLoading }: DeleteProjectModalProps) => {
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
          <Button
            variant="outline"
            onClick={onClose}
            disabled={isLoading}
          >
            Cancel
          </Button>
          <Button
            variant="destructive"
            onClick={onConfirm}
            disabled={isLoading}
            className="flex items-center gap-2"
          >
            {isLoading && <Loader2 className="h-4 w-4 animate-spin" />}
            {isLoading ? 'Deleting...' : 'Delete'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

export default DeleteProjectModal;
