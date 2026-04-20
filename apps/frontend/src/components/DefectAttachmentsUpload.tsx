import { useState, useRef } from 'react';
import { Upload, FileText, Image, File, X } from 'lucide-react';

interface AttachmentFile {
  id?: string; // For existing attachments
  file?: File; // For new uploads
  name: string;
  size: number;
  type: string;
}

interface DefectAttachmentsUploadProps {
  files: AttachmentFile[];
  onFilesChange: (files: AttachmentFile[]) => void;
  readonly?: boolean;
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function getFileIcon(type: string) {
  if (type.startsWith('image/')) return Image;
  if (type.includes('pdf') || type.includes('document') || type.includes('text')) return FileText;
  return File;
}

const DefectAttachmentsUpload = ({ files, onFilesChange, readonly = false }: DefectAttachmentsUploadProps) => {
  const [dragOver, setDragOver] = useState(false);
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

  if (!readonly && files.length === 0) {
    return (
      <div>
        <div
          className={`mt-1.5 border-2 border-dashed border-border rounded-lg p-3 text-center cursor-pointer transition-colors ${
            dragOver ? 'border-primary/40 bg-primary/5' : 'hover:border-primary/40 hover:bg-primary/5'
          }`}
          onClick={() => fileInputRef.current?.click()}
          onDragOver={(e) => {
            e.preventDefault();
            e.stopPropagation();
            setDragOver(true);
          }}
          onDragLeave={() => setDragOver(false)}
          onDrop={(e) => {
            e.preventDefault();
            e.stopPropagation();
            setDragOver(false);
            handleFileUpload(e.dataTransfer.files);
          }}
        >
          <Upload className="h-5 w-5 text-muted-foreground mx-auto mb-1" strokeWidth={1.5} />
          <p className="text-xs text-foreground font-medium">Drop files or click to browse</p>
          <p className="text-[10px] text-muted-foreground mt-0.5">Screenshots, logs, PDFs</p>
          <input
            ref={fileInputRef}
            type="file"
            multiple
            className="hidden"
            onChange={(e) => handleFileUpload(e.target.files)}
          />
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-2">
      {/* Upload zone (Edit mode only) */}
      {!readonly && (
        <div
          className={`mt-1.5 border-2 border-dashed border-border rounded-lg p-3 text-center cursor-pointer transition-colors ${
            dragOver ? 'border-primary/40 bg-primary/5' : 'hover:border-primary/40 hover:bg-primary/5'
          }`}
          onClick={() => fileInputRef.current?.click()}
          onDragOver={(e) => {
            e.preventDefault();
            e.stopPropagation();
            setDragOver(true);
          }}
          onDragLeave={() => setDragOver(false)}
          onDrop={(e) => {
            e.preventDefault();
            e.stopPropagation();
            setDragOver(false);
            handleFileUpload(e.dataTransfer.files);
          }}
        >
          <Upload className="h-4 w-4 text-muted-foreground mx-auto mb-1" strokeWidth={1.5} />
          <p className="text-[11px] text-muted-foreground">
            Drop files or <span className="font-medium text-foreground">browse</span> to add more
          </p>
          <input
            ref={fileInputRef}
            type="file"
            multiple
            className="hidden"
            onChange={(e) => handleFileUpload(e.target.files)}
          />
        </div>
      )}

      {/* File list */}
      {files.length > 0 && (
        <div className="space-y-1.5">
          {files.map((file, i) => {
            const IconComp = getFileIcon(file.type);
            return (
              <div key={i} className="flex items-center gap-2.5 px-3 py-2 bg-secondary rounded-md">
                <IconComp className="h-4 w-4 text-muted-foreground shrink-0" strokeWidth={1.5} />
                <span className="text-sm text-foreground truncate flex-1">{file.name}</span>
                <span className="text-xs text-muted-foreground shrink-0">{formatFileSize(file.size)}</span>
                {!readonly && (
                  <button
                    onClick={() => removeFile(i)}
                    className="text-muted-foreground hover:text-destructive transition-colors"
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

      {/* Empty state (View mode only) */}
      {readonly && files.length === 0 && (
        <p className="text-sm text-muted-foreground mt-1.5">No attachments</p>
      )}
    </div>
  );
};

export default DefectAttachmentsUpload;
