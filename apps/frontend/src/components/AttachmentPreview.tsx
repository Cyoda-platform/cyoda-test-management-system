import { useAuthenticatedImageUrl } from '@/hooks/useAuthenticatedImageUrl';
import { Image } from 'lucide-react';

/** Renders a PDF fetched with Bearer token inside an iframe via blob URL. */
export const AuthenticatedPdf = ({
  url,
  className,
}: {
  url: string;
  className?: string;
}) => {
  const { blobUrl, error } = useAuthenticatedImageUrl(url);
  if (error) return (
    <div className="w-full h-full flex items-center justify-center text-xs text-muted-foreground">
      Failed to load PDF
    </div>
  );
  if (!blobUrl) return (
    <div className="w-full h-full flex items-center justify-center text-xs text-muted-foreground">
      Loading…
    </div>
  );
  return <iframe src={blobUrl} className={className} title="PDF preview" />;
};

/** Renders an image fetched with Bearer token as a blob URL. */
export const AuthenticatedImage = ({
  url,
  alt,
  className,
}: {
  url: string;
  alt: string;
  className?: string;
}) => {
  const { blobUrl, error } = useAuthenticatedImageUrl(url);
  if (error) return (
    <div className="w-full h-full flex items-center justify-center text-xs text-muted-foreground">
      <Image className="h-8 w-8 opacity-30" strokeWidth={1} />
    </div>
  );
  if (!blobUrl) return (
    <div className="w-full h-full flex items-center justify-center text-xs text-muted-foreground">
      Loading…
    </div>
  );
  return <img src={blobUrl} alt={alt} className={className} />;
};
