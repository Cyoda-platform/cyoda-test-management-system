import { cn } from '@/lib/utils';

const priorityStyles = {
  HIGH: 'text-destructive',
  MEDIUM: 'text-warning',
  LOW: 'text-muted-foreground',
} as const;

interface PriorityBadgeProps {
  priority: string;
  className?: string;
}

const PriorityBadge = ({ priority, className }: PriorityBadgeProps) => {
  const key = (priority?.toUpperCase() || 'LOW') as keyof typeof priorityStyles;
  return (
    <span className={cn('inline-flex items-center gap-1 px-2.5 py-0.5 text-[10px] uppercase tracking-widest font-mono', priorityStyles[key] || priorityStyles.LOW, className)}>
      {priority}
    </span>
  );
};

export const prioritySelectorStyles = (p: 'HIGH' | 'MEDIUM' | 'LOW', isActive: boolean) =>
  isActive
    ? `${priorityStyles[p]} ring-1 rounded-md ${p === 'HIGH' ? 'ring-destructive/30' : p === 'MEDIUM' ? 'ring-warning/30' : 'ring-muted-foreground/30'}`
    : 'bg-card text-muted-foreground hover:bg-muted rounded-md';

export default PriorityBadge;
