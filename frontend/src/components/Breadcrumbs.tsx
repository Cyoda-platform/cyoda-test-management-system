import { Link } from 'react-router-dom';

export interface BreadcrumbSegment {
  label: string;
  href?: string;
}

interface BreadcrumbsProps {
  segments: BreadcrumbSegment[];
}

const Breadcrumbs = ({ segments }: BreadcrumbsProps) => {
  return (
    <nav aria-label="breadcrumb" className="text-[11px] font-mono uppercase tracking-widest font-bold flex items-center gap-1.5 flex-wrap">
      {segments.map((seg, i) => {
        const isLast = i === segments.length - 1;
        return (
          <span key={i} className="flex items-center gap-1.5">
            {i > 0 && <span className="text-muted-foreground/50 select-none">/</span>}
            {isLast || !seg.href ? (
              <span className={isLast ? 'text-muted-foreground' : 'text-foreground'}>{seg.label}</span>
            ) : (
              <Link
                to={seg.href}
                className="text-foreground hover:text-accent transition-colors"
              >
                {seg.label}
              </Link>
            )}
          </span>
        );
      })}
    </nav>
  );
};

export default Breadcrumbs;
