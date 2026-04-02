import { useState, useEffect } from 'react';
import { Lock } from 'lucide-react';
import { listDisplayId } from '@/lib/utils';
import {
  Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Label } from '@/components/ui/label';

interface ProjectModalProps {
  open: boolean;
  onClose: () => void;
  onSave: (name: string, description: string) => void;
  mode: 'create' | 'edit';
  initialName?: string;
  initialDescription?: string;
  projectId?: string;
  projectIndex?: number;
}

const ProjectModal = ({ open, onClose, onSave, mode, initialName = '', initialDescription = '', projectId, projectIndex }: ProjectModalProps) => {
  const [name, setName] = useState(initialName);
  const [description, setDescription] = useState(initialDescription);

  useEffect(() => {
    setName(initialName);
    setDescription(initialDescription);
  }, [initialName, initialDescription, open]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    onSave(name.trim(), description.trim());
  };

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-lg glass-surface border-0">
        <DialogHeader>
          <DialogTitle className="tracking-[-0.02em]">{mode === 'create' ? 'Create New Project' : 'Edit Project'}</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <Label className="text-[10px] font-mono uppercase tracking-widest text-muted-foreground">Project Name <span className="text-destructive">*</span></Label>
            <Input
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Enter project name"
              className="bg-secondary border-0 focus-visible:ring-1 focus-visible:ring-accent/40"
            />
          </div>
          <div className="space-y-1.5">
            <Label className="text-[10px] font-mono uppercase tracking-widest text-muted-foreground">Description</Label>
            <Textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Describe the scope and objectives..."
              rows={3}
              className="bg-secondary border-0 focus-visible:ring-1 focus-visible:ring-accent/40"
            />
          </div>
          <div className="space-y-1.5">
            <Label className="text-[10px] font-mono uppercase tracking-widest text-muted-foreground">Project ID</Label>
            <div className="relative">
              <Input
                value={mode === 'edit' && projectIndex !== undefined ? listDisplayId('PROJ', projectIndex) : 'PROJ-XXXX'}
              title={mode === 'edit' ? projectId : undefined}
                disabled
                className="bg-secondary pr-10 border-0 font-mono text-xs"
              />
              <Lock className="absolute right-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" strokeWidth={1.5} />
            </div>
          </div>
          <DialogFooter>
            <Button type="button" variant="ghost" onClick={onClose}>Cancel</Button>
            <Button type="submit" className="bg-gradient-to-br from-primary to-primary/80 text-primary-foreground border-0">
              {mode === 'create' ? 'Create Project' : 'Save Changes'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
};

export default ProjectModal;
