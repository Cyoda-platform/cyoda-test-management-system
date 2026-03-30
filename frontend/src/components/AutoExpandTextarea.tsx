import { useRef, useEffect, useCallback } from 'react';
import { cn } from '@/lib/utils';

interface AutoExpandTextareaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  value: string;
  onValueChange?: (value: string) => void;
}

const AutoExpandTextarea = ({ className, value, onValueChange, onChange, ...props }: AutoExpandTextareaProps) => {
  const ref = useRef<HTMLTextAreaElement>(null);

  const adjust = useCallback(() => {
    const el = ref.current;
    if (!el) return;
    el.style.height = 'auto';
    el.style.height = `${Math.max(el.scrollHeight, 56)}px`;
  }, []);

  useEffect(() => {
    adjust();
  }, [value, adjust]);

  return (
    <textarea
      ref={ref}
      className={cn(
        'w-full rounded-md border border-input bg-white px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:border-foreground/30 transition-colors resize-none overflow-hidden',
        className,
      )}
      value={value}
      onChange={(e) => {
        onChange?.(e);
        onValueChange?.(e.target.value);
        adjust();
      }}
      {...props}
    />
  );
};

export default AutoExpandTextarea;
