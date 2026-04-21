import { useAuthenticatedImageUrl } from '@/hooks/useAuthenticatedImageUrl';
import { getAuthToken } from '@/lib/api';
import { FileText, Image, File } from 'lucide-react';

const BASE_URL = import.meta.env.VITE_API_URL ??
  (import.meta.env.DEV ? 'http://localhost:8080/api' : '/api');

export function attachmentContentUrl(projectId: string, attachmentId: string): string {
  return `${BASE_URL}/projects/${projectId}/attachments/${attachmentId}/content`;
}

export function downloadWithAuth(url: string, fileName: string): void {
  const token = getAuthToken();
  const headers: HeadersInit = token ? { Authorization: `Bearer ${token}` } : {};
  fetch(url, { credentials: 'include', headers })
    .then(r => r.ok ? r.blob() : Promise.reject(r.status))
    .then(blob => {
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = fileName;
      a.click();
      URL.revokeObjectURL(a.href);
    })
    .catch(() => {});
}

export function getFileIconComponent(mimeType: string | null | undefined) {
  if (!mimeType) return File;
  if (mimeType.startsWith('image/')) return Image;
  if (mimeType.startsWith('text/') || mimeType.includes('pdf') || mimeType.includes('document')) return FileText;
  return File;
}

export function isImageType(mimeType: string | null | undefined): boolean {
  return !!mimeType && mimeType.startsWith('image/');
}

export function isPdfType(mimeType: string | null | undefined): boolean {
  return mimeType === 'application/pdf';
}

export function isPreviewableType(mimeType: string | null | undefined): boolean {
  return isImageType(mimeType) || isPdfType(mimeType);
}

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
