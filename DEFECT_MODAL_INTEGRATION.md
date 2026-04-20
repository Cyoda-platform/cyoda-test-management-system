# Defect Modal Integration Guide

## Overview

Three new modal components replace the inline modals in Defects.tsx and RunExecution.tsx:
- `EditDefectModal` — editable version with file upload
- `ViewDefectModal` — read-only version
- `CreateDefectModal` — existing component (minor style alignment)
- `DefectAttachmentsUpload` — reusable file upload component

---

## Component APIs

### EditDefectModal

```typescript
interface EditDefectModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  defect: Defect | null;
  displayId?: string; // e.g., "DEF-1"
  existingAttachments?: AttachmentFile[];
  onSave: (defect: Defect, newFiles: File[], removedAttachmentIds: string[]) => Promise<void>;
}
```

**Usage:**
```tsx
const [editOpen, setEditOpen] = useState(false);
const [editTarget, setEditTarget] = useState<Defect | null>(null);

<EditDefectModal
  open={editOpen}
  onOpenChange={setEditOpen}
  defect={editTarget}
  displayId={defectDisplayIdMap[editTarget?.id]}
  existingAttachments={existingAttachments}
  onSave={async (defect, newFiles, removedIds) => {
    // 1. Update defect via API
    await updateDefect(defect);
    
    // 2. Upload new files
    for (const file of newFiles) {
      await uploadAttachment(projectId, defect.id, file);
    }
    
    // 3. Delete removed attachments
    for (const id of removedIds) {
      await deleteAttachment(projectId, defect.id, id);
    }
    
    // 4. Refetch or update state
    setEditOpen(false);
  }}
/>
```

---

### ViewDefectModal

```typescript
interface ViewDefectModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  defect: Defect | null;
  displayId?: string;
  existingAttachments?: AttachmentFile[];
  formatDate?: (date: string) => string;
}
```

**Usage:**
```tsx
const [viewOpen, setViewOpen] = useState(false);
const [viewTarget, setViewTarget] = useState<Defect | null>(null);

<ViewDefectModal
  open={viewOpen}
  onOpenChange={setViewOpen}
  defect={viewTarget}
  displayId={defectDisplayIdMap[viewTarget?.id]}
  existingAttachments={existingAttachments}
  formatDate={formatDate} // Your date formatting function
/>
```

---

### DefectAttachmentsUpload

```typescript
interface DefectAttachmentsUploadProps {
  files: AttachmentFile[];
  onFilesChange: (files: AttachmentFile[]) => void;
  readonly?: boolean;
}

interface AttachmentFile {
  id?: string; // For existing attachments (from server)
  file?: File; // For new uploads (from user)
  name: string;
  size: number;
  type: string;
}
```

**Usage (Edit Mode):**
```tsx
const [attachments, setAttachments] = useState<AttachmentFile[]>(existingAttachments);

<DefectAttachmentsUpload
  files={attachments}
  onFilesChange={setAttachments}
  readonly={false}
/>
```

**Usage (View Mode):**
```tsx
<DefectAttachmentsUpload
  files={existingAttachments}
  onFilesChange={() => {}} // Noop
  readonly={true}
/>
```

---

## Integration: Defects.tsx

### Before (Inline Modal)

```tsx
{/* Edit Defect Modal */}
<Dialog open={editOpen} onOpenChange={setEditOpen}>
  <DialogContent className="sm:max-w-3xl bg-card">
    <DialogHeader>
      <DialogTitle>Edit Defect</DialogTitle>
      <DialogDescription>{defectDisplayIdMap[editTarget?.id]}</DialogDescription>
    </DialogHeader>
    {editTarget && (
      <div className="space-y-4">
        {/* Title, Description, Severity, Status, Source, Link, Attachments fields */}
        <DefectAttachmentsList projectId={projectId!} defectId={editTarget.id} />
      </div>
    )}
    <DialogFooter>
      <Button onClick={() => setEditOpen(false)}>Cancel</Button>
      <Button onClick={handleEdit}>Save Changes</Button>
    </DialogFooter>
  </DialogContent>
</Dialog>
```

### After (Using EditDefectModal)

```tsx
import EditDefectModal from '@/components/EditDefectModal';
import ViewDefectModal from '@/components/ViewDefectModal';

// In component state
const [editOpen, setEditOpen] = useState(false);
const [editTarget, setEditTarget] = useState<Defect | null>(null);
const [editAttachments, setEditAttachments] = useState<AttachmentFile[]>([]);

const [viewOpen, setViewOpen] = useState(false);
const [viewTarget, setViewTarget] = useState<Defect | null>(null);
const [viewAttachments, setViewAttachments] = useState<AttachmentFile[]>([]);

// Fetch attachments when opening modals
const openEdit = (defect: Defect) => {
  setEditTarget(defect);
  // Fetch attachments
  attachmentsApi.listByDefect(projectId!, defect.id)
    .then(atts => setEditAttachments(atts || []))
    .catch(() => setEditAttachments([]));
  setEditOpen(true);
};

const openView = (defect: Defect) => {
  setViewTarget(defect);
  // Fetch attachments
  attachmentsApi.listByDefect(projectId!, defect.id)
    .then(atts => setViewAttachments(atts || []))
    .catch(() => setViewAttachments([]));
  setViewOpen(true);
};

// In JSX
<EditDefectModal
  open={editOpen}
  onOpenChange={setEditOpen}
  defect={editTarget}
  displayId={editTarget ? defectDisplayIdMap[editTarget.id] : undefined}
  existingAttachments={editAttachments}
  onSave={async (defect, newFiles, removedIds) => {
    try {
      // Update defect
      await updateDefect(defect);
      
      // Upload new files
      for (const file of newFiles) {
        await attachmentsApi.upload(projectId!, defect.id, file);
      }
      
      // Delete removed attachments
      for (const id of removedIds) {
        await attachmentsApi.delete(projectId!, defect.id, id);
      }
      
      // Refetch defects
      await refetchDefects();
      setEditOpen(false);
    } catch (error) {
      toast.error('Failed to save defect');
    }
  }}
/>

<ViewDefectModal
  open={viewOpen}
  onOpenChange={setViewOpen}
  defect={viewTarget}
  displayId={viewTarget ? defectDisplayIdMap[viewTarget.id] : undefined}
  existingAttachments={viewAttachments}
  formatDate={formatDate}
/>
```

---

## Integration: RunExecution.tsx

Same pattern as Defects.tsx:

```tsx
import EditDefectModal from '@/components/EditDefectModal';
import ViewDefectModal from '@/components/ViewDefectModal';

// Replace inline modals with the new components
<EditDefectModal
  open={editDefectOpen}
  onOpenChange={setEditDefectOpen}
  defect={selectedDefect}
  displayId={defectDisplayIdMap[selectedDefect?.id]}
  existingAttachments={editAttachments}
  onSave={handleEditDefectSave}
/>

<ViewDefectModal
  open={viewDefectOpen}
  onOpenChange={setViewDefectOpen}
  defect={selectedDefect}
  displayId={defectDisplayIdMap[selectedDefect?.id]}
  existingAttachments={viewAttachments}
  formatDate={formatDate}
/>
```

---

## API Endpoints Required

The modals assume these API endpoints exist:

```typescript
// Upload attachment
POST /projects/:projectId/defects/:defectId/attachments
Headers: Content-Type: multipart/form-data
Body: { file: File }

// List attachments
GET /projects/:projectId/defects/:defectId/attachments
Response: { id: string, fileName: string, fileSize: number, fileType: string }[]

// Delete attachment
DELETE /projects/:projectId/defects/:defectId/attachments/:attachmentId

// Update defect
PUT /projects/:projectId/defects/:defectId
Body: { title, description, severity, status, source, link }
```

---

## UpdatedAttachmentFile Interface

```typescript
interface AttachmentFile {
  id?: string;           // Provided by server, undefined for new uploads
  file?: File;           // Provided by user, undefined for existing attachments
  name: string;          // Always required (file.name or serverFileName)
  size: number;          // Always required (file.size or serverFileSize)
  type: string;          // Always required (file.type or serverFileType)
}
```

---

## CreateDefectModal Updates

Minor alignment to ensure consistency:

```tsx
// Current implementation already matches:
// - Size: sm:max-w-3xl ✓
// - Label style: uppercase, monospace ✓
// - File upload: drag & drop with Upload icon ✓
// - Input styles: h-9, border, focus ring ✓

// No major changes needed, but verify:
// 1. File list styling matches DefectAttachmentsUpload
// 2. Icon library uses consistent imports (lucide-react)
```

---

## Styling Consistency Checklist

- [x] Modal size: `sm:max-w-3xl`
- [x] Label style: `text-[10px] font-semibold text-muted-foreground uppercase mb-1.5 block font-mono tracking-widest`
- [x] Input style: `h-9 bg-white border border-input focus-visible:ring-1 focus-visible:ring-ring`
- [x] Textarea style: `min-h-[140px] bg-white border border-input focus-visible:ring-1 focus-visible:ring-ring text-sm resize-none`
- [x] Read-only field style: `h-9 px-3 bg-white border border-input rounded-md text-sm text-foreground flex items-center`
- [x] File upload zone: border-dashed, drag & drop support
- [x] File list: bg-secondary rounded-md with icon, name, size, delete button
- [x] Attachments label: "Attachments"
- [x] Grid layout: 3-column for classification fields, 2-column where applicable

---

## Testing Checklist

- [ ] EditDefectModal: Create → edit → upload file → save → verify API calls
- [ ] EditDefectModal: Load existing defect → edit fields → remove attachment → save
- [ ] ViewDefectModal: Display all fields as read-only
- [ ] ViewDefectModal: Show external link as clickable (target="_blank")
- [ ] DefectAttachmentsUpload: Drag & drop multiple files
- [ ] DefectAttachmentsUpload: Click to browse and select files
- [ ] DefectAttachmentsUpload: Delete button removes file from list
- [ ] Defects.tsx: openEdit → loads attachments → saves with new files
- [ ] Defects.tsx: openView → loads attachments → displays read-only
- [ ] RunExecution.tsx: Same as Defects.tsx
- [ ] CreateDefectModal: Still works, aligns with other modals

---

## Migration Steps

1. **Create new components** (Done)
   - DefectAttachmentsUpload.tsx
   - EditDefectModal.tsx
   - ViewDefectModal.tsx

2. **Update Defects.tsx**
   - Import new modals
   - Replace inline Edit modal with `<EditDefectModal />`
   - Replace inline View modal with `<ViewDefectModal />`
   - Update state management for attachments

3. **Update RunExecution.tsx**
   - Same as Defects.tsx

4. **Update CreateDefectModal** (optional alignment)
   - Verify styling consistency
   - Consider refactoring file upload to use DefectAttachmentsUpload

5. **Test thoroughly**
   - All create/edit/view flows
   - File upload/delete
   - API integration
   - Cross-page consistency

6. **Deploy & monitor**
   - Check for errors in production
   - Verify UI consistency across pages
