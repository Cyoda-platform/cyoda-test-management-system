# Defect Modal Design Specification

## Overview
Unify the design of all defect modals (Create, Edit, View) across the application — whether opened from the Defects page or Test Run page.

---

## Modal Structure

### Base Properties
- **Size**: `sm:max-w-3xl` (768px width, responsive)
- **Background**: `bg-card`
- **Spacing**: `space-y-4` between sections
- **Label Style**: Uppercase, small, monospace, muted text
  ```
  text-[10px] font-semibold text-muted-foreground uppercase mb-1.5 block font-mono tracking-widest
  ```

### Header
- **Title**: Primary heading (e.g., "Edit Defect", "Create Defect", "View Defect")
- **Subtitle**: Secondary info
  - Create: "Report a new issue."
  - Edit: Defect ID (e.g., "DEF-1")
  - View: "Defect details"

### Content Grid

#### Section 1: Identification
- **Title** (full width, editable/read-only)
- **Description** (full width, textarea, editable/read-only)

#### Section 2: Classification (3-column grid)
- **Severity** (dropdown: Critical, Major, Minor)
- **Status** (dropdown: Open, In Progress, Fixed, Closed)
- **Source** (text input: "TR-01 or AS-1")

#### Section 3: Links
- **External Link** (full width, text input, editable/read-only)

#### Section 4: Attachments (Edit & Create modes only)
- Upload field with drag & drop
- List of attached files below
- **NOT visible in View mode**

---

## Mode-Specific Styling

### CREATE & EDIT Modes
- **Input fields**: 
  - Height: `h-9`
  - Background: `bg-white`
  - Border: `border border-input`
  - Focus: `focus-visible:ring-1 focus-visible:ring-ring`
- **Textarea**: 
  - Min-height: `min-h-[140px]`
  - Resize disabled: `resize-none`
- **Attachments field** (Edit mode only):
  - Drag & drop zone with dashed border
  - Icon: `Paperclip` or `Upload` (consistent with CreateDefectModal)
  - Placeholder text for empty state
  - File list with delete buttons

### VIEW Mode
- **All fields are read-only** (no input, no select, no upload)
- Fields styled as **display-only boxes**:
  - Height: `h-9` (or `min-h-[140px]` for description)
  - Background: `bg-white`
  - Border: `border border-input`
  - Padding: `px-3 py-2`
  - Overflow: `overflow-y-auto` (description only)
  - Content: Regular text or formatted values
- **No attachments upload field** — only show existing attachments
- **Links as clickable anchors** (with `ExternalLink` icon)

### Attachment Handling

#### Upload Field (Create & Edit)
```
- Zone style: border-2 border-dashed, rounded-lg, p-3
- Active state: border-primary/40, bg-primary/5
- Icon: Upload (lucide-react)
- Text: "Drop files or click to browse"
- Subtext: "Screenshots, logs, PDFs"
- Support: Multiple file upload via drag & drop or click
```

#### File List (All Modes)
```
- Container: space-y-1.5
- Item style:
  - Edit/Create: flex items-center gap-2.5 px-3 py-2 bg-secondary rounded-md
  - View: same, but without delete button
- Icon: getFileIcon() based on MIME type
- Name: truncated text
- Size: formatFileSize()
- Delete button: X icon (Edit/Create only)
```

---

## Components to Update/Create

### 1. **EditDefectModal** (new component)
- Extract Edit modal logic from Defects.tsx into a reusable component
- Add file upload functionality (`DefectAttachmentsUpload`)
- Props: `open`, `onOpenChange`, `defect`, `onSave`, `projectId`

### 2. **ViewDefectModal** (new component)
- Extract View modal logic from Defects.tsx
- All fields read-only
- Reusable across pages
- Props: `open`, `onOpenChange`, `defect`, `onClose`

### 3. **CreateDefectModal** (existing, align)
- Keep current structure
- Ensure it matches modal size and spacing
- Verify attachment upload matches ViewDefectModal's file list style

### 4. **DefectAttachmentsUpload** (new component)
- Drag & drop file upload zone
- File list with delete buttons
- Reusable for both Create and Edit
- Props: `files`, `onFilesChange`, `readonly` (for View mode)

### 5. **DefectAttachmentsList** (update)
- Show existing files only
- Add delete capability for Edit mode
- Props: `projectId`, `defectId`, `editable`, `onFileRemove`

---

## Common Utilities

```typescript
// Label class
const labelCls = 'text-[10px] font-semibold text-muted-foreground uppercase mb-1.5 block font-mono tracking-widest';

// Input class
const inputCls = 'mt-0 h-9 bg-white border border-input focus-visible:ring-1 focus-visible:ring-ring';

// Read-only field class
const readOnlyFieldCls = 'mt-0 h-9 px-3 bg-white border border-input rounded-md text-sm text-foreground flex items-center';

// Textarea class
const textareaCls = 'mt-0 min-h-[140px] bg-white border border-input focus-visible:ring-1 focus-visible:ring-ring text-sm resize-none';
```

---

## Integration Points

### Defects.tsx
- Replace inline Edit modal with `<EditDefectModal />`
- Replace inline View modal with `<ViewDefectModal />`
- Manage `editOpen`, `editTarget` → pass to `EditDefectModal`
- Manage `viewOpen`, `viewTarget` → pass to `ViewDefectModal`

### RunExecution.tsx
- Replace any inline defect modals with the same unified components
- Props should match Defects.tsx usage

---

## Validation & Attachments API

### File Upload (Edit mode)
- Add files to the Edit modal's local state
- On "Save Changes", upload new files via API
- Remove deleted files via API

### Defect Attachment API
- `POST /projects/:projectId/defects/:defectId/attachments` — upload new file
- `DELETE /projects/:projectId/defects/:defectId/attachments/:attachmentId` — remove file
- `GET /projects/:projectId/defects/:defectId/attachments` — list files

---

## Accessibility

- Form labels associated with inputs
- Read-only fields clearly identified (no input focus)
- File upload zone has hover state and keyboard support
- Color contrast meets WCAG AA
- Icon-only buttons have accessible labels (tooltips or aria-label)
