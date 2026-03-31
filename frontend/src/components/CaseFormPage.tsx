import { useState, useCallback, useRef } from 'react';
import Breadcrumbs from '@/components/Breadcrumbs';
import { useNavigate } from 'react-router-dom';
import { Plus, Trash2, GripVertical, Upload, FileText, X, Paperclip } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { prioritySelectorStyles } from '@/components/PriorityBadge';
import AutoExpandTextarea from '@/components/AutoExpandTextarea';
import type { LocalCase as TestCase, LocalStep as TestStep, LocalSuite as Suite } from '@/lib/localTypes';

interface Attachment {
  id: string;
  name: string;
  size: number;
  type: string;
}

interface CaseFormPageProps {
  mode: 'create' | 'edit';
  suites: Suite[];
  initialSuiteId: string;
  initialCase?: TestCase;
  projectName?: string;
  projectId?: string;
  onSave: (suiteId: string, data: { title: string; priority: 'HIGH' | 'MEDIUM' | 'LOW'; description: string; preconditions: string; steps: TestStep[] }) => void;
  onCancel: () => void;
}

const inputCls = 'flex h-10 w-full rounded-md border border-input bg-white px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:border-foreground/30 transition-colors';
const textareaCls = 'w-full rounded-md border border-input bg-white px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:border-foreground/30 transition-colors resize-none overflow-hidden';
const labelCls = 'text-[10px] font-semibold text-muted-foreground uppercase mb-1.5 block font-mono tracking-widest';

const formatSize = (bytes: number) => {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1048576) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1048576).toFixed(1)} MB`;
};

const CaseFormPage = ({ mode, suites, initialSuiteId, initialCase, projectName, projectId, onSave, onCancel }: CaseFormPageProps) => {
  const navigate = useNavigate();
  const [suiteId, setSuiteId] = useState(initialSuiteId);
  const [title, setTitle] = useState(initialCase?.title || '');
  const [priority, setPriority] = useState<'HIGH' | 'MEDIUM' | 'LOW'>(initialCase?.priority || 'MEDIUM');
  const [description, setDescription] = useState(initialCase?.description || '');
  const [preconditions, setPreconditions] = useState(initialCase?.preconditions || '');
  const [steps, setSteps] = useState<TestStep[]>(
    initialCase?.steps?.length
      ? initialCase.steps.map((s, i) => ({ ...s, order: i + 1 }))
      : []
  );
  const [attachments, setAttachments] = useState<Attachment[]>([]);
  const [dragOver, setDragOver] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [dragIndex, setDragIndex] = useState<number | null>(null);
  const [dragOverIndex, setDragOverIndex] = useState<number | null>(null);

  // Steps CRUD
  const addStep = () => {
    setSteps((prev) => [
      ...prev,
      { order: prev.length + 1, action: '', expectedResult: '', status: 'untested' as const },
    ]);
  };

  const updateStep = (index: number, field: 'action' | 'expectedResult', value: string) => {
    setSteps((prev) => prev.map((s, i) => (i === index ? { ...s, [field]: value } : s)));
  };

  const removeStep = (index: number) => {
    setSteps((prev) => prev.filter((_, i) => i !== index).map((s, i) => ({ ...s, order: i + 1 })));
  };

  // Step drag-and-drop
  const handleDragStart = (index: number) => setDragIndex(index);
  const handleDragOver = useCallback((e: React.DragEvent, index: number) => {
    e.preventDefault();
    setDragOverIndex(index);
  }, []);
  const handleDrop = (index: number) => {
    if (dragIndex === null || dragIndex === index) {
      setDragIndex(null);
      setDragOverIndex(null);
      return;
    }
    setSteps((prev) => {
      const next = [...prev];
      const [moved] = next.splice(dragIndex, 1);
      next.splice(index, 0, moved);
      return next.map((s, i) => ({ ...s, order: i + 1 }));
    });
    setDragIndex(null);
    setDragOverIndex(null);
  };
  const handleDragEnd = () => { setDragIndex(null); setDragOverIndex(null); };

  // Attachments
  const addFiles = (files: FileList | null) => {
    if (!files) return;
    const newAttachments: Attachment[] = Array.from(files).map((f) => ({
      id: `att-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
      name: f.name,
      size: f.size,
      type: f.type,
    }));
    setAttachments((prev) => [...prev, ...newAttachments]);
  };

  const removeAttachment = (id: string) => {
    setAttachments((prev) => prev.filter((a) => a.id !== id));
  };

  const handleFileDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    addFiles(e.dataTransfer.files);
  };

  const handleSubmit = () => {
    if (!title.trim() || !suiteId) return;
    onSave(suiteId, {
      title: title.trim(),
      priority,
      description: description.trim(),
      preconditions: preconditions.trim(),
      steps,
    });
  };

  return (
    <div className="h-full flex flex-col">
      {/* Scrollable form body */}
      <div className="flex-1 overflow-auto">
        <div className="max-w-5xl mx-auto px-8 py-6 space-y-5">
          {/* Breadcrumbs */}
          <Breadcrumbs segments={[
            { label: 'Projects', href: '/projects' },
            { label: projectName || 'Project', href: `/projects/${projectId}/repository` },
            { label: 'Repository', href: `/projects/${projectId}/repository` },
            { label: mode === 'create' ? 'Create Case' : 'Edit Case' },
          ]} />

          <h1 className="text-xl font-bold text-foreground tracking-[-0.02em]">
            {mode === 'create' ? 'Create Test Case' : 'Edit Test Case'}
          </h1>

          {/* Row 1: Suite / Priority */}
          <div className="grid grid-cols-2 gap-5">
            <div>
              <label className={labelCls}>Suite</label>
              <select className={inputCls} value={suiteId} onChange={(e) => setSuiteId(e.target.value)}>
                {suites.map((s) => (
                  <option key={s.id} value={s.id}>{s.name}</option>
                ))}
              </select>
            </div>
            <div>
              <label className={labelCls}>Priority</label>
              <div className="flex gap-2 h-10 items-center">
                {(['HIGH', 'MEDIUM', 'LOW'] as const).map((p) => (
                  <button
                    key={p}
                    type="button"
                    onClick={() => setPriority(p)}
                    className={`px-3 py-1.5 rounded-md text-xs font-bold uppercase tracking-wide transition-colors ${prioritySelectorStyles(p, priority === p)}`}
                  >
                    {p}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Row 2: Title / Attachments */}
          <div className="grid grid-cols-2 gap-5">
            <div>
              <label className={labelCls}>Title *</label>
              <input
                className={inputCls}
                placeholder="e.g. Login with valid credentials"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                autoFocus
              />
            </div>
            <div>
              <label className={labelCls}>Attachments</label>
              <div
                className={`rounded-md border border-dashed transition-colors h-10 px-3 flex items-center gap-3 ${
                  dragOver ? 'border-primary bg-primary/5' : 'border-input bg-white'
                }`}
                onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
                onDragLeave={() => setDragOver(false)}
                onDrop={handleFileDrop}
              >
                <Paperclip className="h-4 w-4 text-muted-foreground shrink-0" strokeWidth={1.5} />
                <span className="text-xs text-muted-foreground flex-1 text-center truncate">
                  {attachments.length > 0
                    ? `${attachments.length} file${attachments.length > 1 ? 's' : ''} attached`
                    : 'Drop files or'}
                </span>
                <button
                  type="button"
                  className="text-xs text-muted-foreground/70 font-medium hover:text-muted-foreground hover:underline shrink-0"
                  onClick={() => fileInputRef.current?.click()}
                >
                  browse
                </button>
                <input
                  ref={fileInputRef}
                  type="file"
                  multiple
                  className="hidden"
                  onChange={(e) => addFiles(e.target.files)}
                />
              </div>
              {attachments.length > 0 && (
                <div className="mt-1.5 flex flex-wrap gap-1.5">
                  {attachments.map((att) => (
                    <span
                      key={att.id}
                      className="inline-flex items-center gap-1.5 rounded-md border border-input bg-white px-2 py-0.5 text-[11px] text-foreground"
                    >
                      <FileText className="h-3 w-3 text-muted-foreground" strokeWidth={1.5} />
                      <span className="max-w-[120px] truncate">{att.name}</span>
                      <button type="button" className="text-muted-foreground hover:text-destructive" onClick={() => removeAttachment(att.id)}>
                        <X className="h-3 w-3" strokeWidth={1.5} />
                      </button>
                    </span>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Row 3: Description / Pre-conditions */}
          <div className="grid grid-cols-2 gap-5">
            <div>
              <label className={labelCls}>Description</label>
              <textarea
                className={`${textareaCls} min-h-[72px]`}
                placeholder="Optional description..."
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>
            <div>
              <label className={labelCls}>Pre-conditions</label>
              <textarea
                className={`${textareaCls} min-h-[72px]`}
                placeholder="Optional pre-conditions..."
                value={preconditions}
                onChange={(e) => setPreconditions(e.target.value)}
              />
            </div>
          </div>

          {/* Row 3: Test Steps — MAIN FOCUS */}
          <div>
            <div className="flex items-center justify-between mb-2">
              <label className={`${labelCls} mb-0`}>Test Steps</label>
              <Button
                type="button"
                variant="outline"
                size="sm"
                className="gap-1.5 text-xs"
                onClick={addStep}
              >
                <Plus className="h-3.5 w-3.5" strokeWidth={1.5} /> Add Step
              </Button>
            </div>

            <div className="bg-white rounded-lg border border-input overflow-hidden">
              {/* Table header */}
              <div className="grid grid-cols-[32px_48px_1fr_1fr_40px] gap-2 bg-muted/40 px-2 py-2 text-[10px] font-semibold text-muted-foreground uppercase font-mono tracking-widest border-b border-input">
                <span></span>
                <span>#</span>
                <span>Step</span>
                <span>Expected result</span>
                <span></span>
              </div>

              {/* Rows */}
              {steps.map((step, idx) => (
                <div
                  key={idx}
                  draggable
                  onDragStart={() => handleDragStart(idx)}
                  onDragOver={(e) => handleDragOver(e, idx)}
                  onDrop={() => handleDrop(idx)}
                  onDragEnd={handleDragEnd}
                  className={`grid grid-cols-[32px_48px_1fr_1fr_40px] gap-2 items-start px-2 py-2.5 border-b border-input last:border-b-0 transition-colors ${
                    dragOverIndex === idx ? 'bg-primary/5' : ''
                  } ${dragIndex === idx ? 'opacity-40' : ''}`}
                >
                  <div className="flex items-center justify-center pt-2.5 cursor-grab active:cursor-grabbing text-muted-foreground hover:text-foreground">
                    <GripVertical className="h-4 w-4" strokeWidth={1.5} />
                  </div>
                  <div className="flex items-center pt-2.5 text-xs font-mono text-muted-foreground font-medium">
                    {step.order}
                  </div>
                  <div className="pr-2">
                    <AutoExpandTextarea
                      placeholder="Describe the step..."
                      value={step.action}
                      onValueChange={(v) => updateStep(idx, 'action', v)}
                      className="min-h-[64px] text-sm"
                    />
                  </div>
                  <div className="pr-1">
                    <AutoExpandTextarea
                      placeholder="Expected result..."
                      value={step.expectedResult}
                      onValueChange={(v) => updateStep(idx, 'expectedResult', v)}
                      className="min-h-[64px] text-sm"
                    />
                  </div>
                  <div className="flex items-center justify-center pt-2.5">
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      className="h-7 w-7 text-muted-foreground hover:text-destructive"
                      onClick={() => removeStep(idx)}
                    >
                      <Trash2 className="h-3.5 w-3.5" strokeWidth={1.5} />
                    </Button>
                  </div>
                </div>
              ))}

              {steps.length === 0 && (
                <div className="px-4 py-10 text-center text-sm text-muted-foreground">
                  No steps yet. Click "+ Add Step" to begin defining test steps.
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Sticky footer */}
      <div className="shrink-0 border-t border-border bg-card px-8 py-3 flex items-center justify-end gap-3">
        <Button variant="ghost" size="sm" onClick={onCancel}>
          Cancel
        </Button>
        <Button
          size="sm"
          className="bg-primary text-primary-foreground hover:bg-primary/90 border-0 px-6"
          onClick={handleSubmit}
          disabled={!title.trim() || !suiteId}
        >
          {mode === 'create' ? 'Create Case' : 'Save Changes'}
        </Button>
      </div>
    </div>
  );
};

export default CaseFormPage;
