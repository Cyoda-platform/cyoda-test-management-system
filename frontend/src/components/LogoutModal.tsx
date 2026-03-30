import { useNavigate } from 'react-router-dom';
import { LogOut } from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';

interface LogoutModalProps {
  open: boolean;
  onClose: () => void;
}

const LogoutModal = ({ open, onClose }: LogoutModalProps) => {
  const navigate = useNavigate();

  const handleLogout = () => {
    onClose();
    navigate('/');
  };

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-md glass-surface border-0">
        <DialogHeader className="items-center text-center">
          <div className="mx-auto mb-2 h-12 w-12 rounded-md bg-destructive/10 flex items-center justify-center">
            <LogOut className="h-6 w-6 text-destructive" strokeWidth={1.5} />
          </div>
          <DialogTitle className="tracking-[-0.02em]">Confirm Logout</DialogTitle>
          <DialogDescription>
            Are you sure you want to log out of CYODA TMS? Any unsaved changes in active forms may be lost.
          </DialogDescription>
        </DialogHeader>
        <DialogFooter className="gap-2 sm:gap-0">
          <Button variant="ghost" onClick={onClose}>Cancel</Button>
          <Button variant="destructive" onClick={handleLogout} className="border-0">Log Out</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

export default LogoutModal;
