import { useState, useRef } from 'react';
import { Upload, FileText, Image, File, X, Download, Eye } from 'lucide-react';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { AuthenticatedImage, AuthenticatedPdf } from '@/components/AttachmentPreview';
import {
  attachmentContentUrl,
  downloadWithAuth,
  isImageType,
  isPdfType,
  isPreviewableType,
  getFileIconComponent,
} from '@/lib/attachmentUtils';

interface AttachmentFile {
  id?: string;
  file?: File;
  name: string;
  size: number;
  type: string;
}

interface DefectAttachmentsUploadProps {
  files: AttachmentFile[];
  onFilesChange: (files: AttachmentFile[]) => void;
  readonly?: boolean;
  projectId?: string;
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

interface PreviewState {
  name: string;
  type: string;
  /** Authenticated URL (server) or blob URL (local File) */
  url: string;
  isLocal: boolean;
}

const DefectAttachmentsUpload = ({
  files,
  onFilesChange,
  readonly = false,
  projectId,
}: DefectAttachmentsUploadProps) => {
  const [dragOver, setDragOver] = useState(false);
  const [preview, setPreview] = useState<PreviewState | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileUpload = (fileList: FileList | null) => {
    if (!fileList || readonly) return;
    const newFiles = Array.from(fileList).map((file) => ({
      file,
      name: file.name,
      size: file.size,
      type: file.type,
    }));
    onFilesChange([...files, ...newFiles]);
  };

  const removeFile = (idx: number) => {
    if (readonly) return;
    onFilesChange(files.filter((_, i) => i !== idx));
  };

  const openPreviewOrDownload = (att: AttachmentFile) => {
    const canShow = isPreviewableType(att.type);

    if (att.file) {
      const localUrl = URL.createObjectURL(att.file);
      if (canShow) {
        setPreview({ name: att.name, type: att.type, url: localUrl, isLocal: true });
      } else {
        downloadWithAuth(localUrl, att.name);
        URL.revokeObjectURL(localUrl);
      }
      return;
    }

    if (att.id && projectId) {
      const url = attachmentContentUrl(projectId, att.id);
      if (canShow) {
        setPreview({ name: att.name, type: att.type, url, isLocal: false });
      } else {
        downloadWithAuth(url, att.name);
      }
    }
  };

  const closePreview = () => {
    if (preview?.isLocal) URL.revokeObjectURL(preview.url);
    setPreview(null);
  };

  const uploadZone = (compact: boolean) => (
    <div
      className={`border-2 border-dashed border-border rounded-lg text-center cursor-pointer transition-colors ${
        compact ? 'p-3' : 'p-4'
      } ${dragOver ? 'border-primary/40 bg-primary/5' : 'hover:border-primary/40 hover:bg-primary/5'}`}
      onClick={() => fileInputRef.current?.click()}
      onDragOver={(e) => { e.preventDefault(); e.stopPropagation(); setDragOver(true); }}
      onDragLeave={() => setDragOver(false)}
      onDrop={(e) => { e.preventDefault(); e.stopPropagation(); setDragOver(false); handleFileUpload(e.dataTransfer.files); }}
    >
      <Upload className={`${compact ? 'h-4 w-4' : 'h-5 w-5'} text-muted-foreground mx-auto mb-1`} strokeWidth={1.5} />
      <p className="text-[11px] text-muted-foreground">
        Drop files or <span className="font-medium text-foreground">browse</span>
        {compact ? ' to add more' : ''}
      </p>
      <input ref={fileInputRef} type="file" multiple className="hidden" onChange={(e) => handleFileUpload(e.target.files)} />
    </div>
  );

  if (!readonly && files.length === 0) {
    return <div className="mt-1.5">{uploadZone(false)}</div>;
  }

  return (
    <>
      <div className="space-y-2">
        {!readonly && <div className="mt-1.5">{uploadZone(true)}</div>}

        {files.length > 0 && (
          <div className="space-y-1.5">
            {files.map((att, i) => {
              const IconComp = getFileIconComponent(att.type);
              const canPreview = isPreviewableType(att.type) && (!!att.file || (!!att.id && !!projectId));
              const canDownload = !!att.file || (!!att.id && !!projectId);
              return (
                <div key={i} className="flex items-center gap-2.5 px-3 py-2 bg-secondary rounded-md">
                  <IconComp className="h-4 w-4 text-muted-foreground shrink-0" strokeWidth={1.5} />
                  <button
                    className={`text-sm text-foreground truncate flex-1 text-left ${canPreview || canDownload ? 'hover:underline cursor-pointer' : ''}`}
                    onClick={() => (canPreview || canDownload) ? openPreviewOrDownload(att) : undefined}
                  >
                    {att.name}
                  </button>
                  <span className="text-xs text-muted-foreground shrink-0">{formatFileSize(att.size)}</span>
                  {canDownload && (
                    <button
                      onClick={() => {
                        if (att.file) {
                          const url = URL.createObjectURL(att.file);
                          downloadWithAuth(url, att.name);
                          URL.revokeObjectURL(url);
                        } else if (att.id && projectId) {
                          downloadWithAuth(attachmentContentUrl(projectId, att.id), att.name);
                        }
                      }}
                      className="text-muted-foreground hover:text-foreground transition-colors shrink-0"
                      title="Download"
                    >
                      <Download className="h-3.5 w-3.5" strokeWidth={1.5} />
                    </button>
                  )}
                  {canPreview && (
                    <button
                      onClick={() => openPreviewOrDownload(att)}
                      className="text-muted-foreground hover:text-foreground transition-colors shrink-0"
                      title="Preview"
                    >
                      <Eye className="h-3.5 w-3.5" strokeWidth={1.5} />
                    </button>
                  )}
                  {!readonly && (
                    <button
                      onClick={() => removeFile(i)}
                      className="text-muted-foreground hover:text-destructive transition-colors shrink-0"
                      aria-label="Remove file"
                    >
                      <X className="h-3.5 w-3.5" strokeWidth={1.5} />
                    </button>
                  )}
                </div>
              );
            })}
          </div>
        )}

        {readonly && files.length === 0 && (
          <p className="text-sm text-muted-foreground mt-1.5">No attachments</p>
        )}
      </div>

      {/* Preview Modal */}
      <Dialog open={!!preview} onOpenChange={(open) => { if (!open) closePreview(); }}>
        <DialogContent className="sm:max-w-2xl bg-background">
          <DialogHeader>
            <DialogTitle className="text-base font-semibold truncate">{preview?.name}</DialogTitle>
            <DialogDescription className="sr-only">File preview</DialogDescription>
          </DialogHeader>
          <div className="flex items-center justify-center min-h-[300px] bg-muted/20 rounded-lg overflow-hidden">
            {preview && isPdfType(preview.type) ? (
              preview.isLocal
                ? <iframe src={preview.url} className="w-full h-[60vh]" title="PDF preview" />
                : <AuthenticatedPdf url={preview.url} className="w-full h-[60vh]" />
            ) : preview && preview.isLocal ? (
              <img src={preview.url} alt={preview.name} className="max-w-full max-h-[60vh] object-contain" />
            ) : preview ? (
              <AuthenticatedImage url={preview.url} alt={preview.name} className="max-w-full max-h-[60vh] object-contain" />
            ) : null}
          </div>
          <DialogFooter>
            <Button variant="ghost" onClick={closePreview}>Close</Button>
            {preview && (
              <Button className="gap-1.5" onClick={() => {
                if (preview.isLocal) {
                  const a = document.createElement('a');
                  a.href = preview.url;
                  a.download = preview.name;
                  a.click();
                } else {
                  downloadWithAuth(preview.url, preview.name);
                }
              }}>
                <Download className="h-3.5 w-3.5" /> Download
              </Button>
            )}
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
};

export default DefectAttachmentsUpload;
