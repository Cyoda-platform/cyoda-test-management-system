import { useState, useCallback, useMemo, useEffect, useRef } from 'react';
import Breadcrumbs from '@/components/Breadcrumbs';
import { useParams, useNavigate } from 'react-router-dom';
import { ChevronDown, ChevronRight, Plus, Pencil, Copy, Trash2, MoreHorizontal, Download, Upload, X, AlertTriangle, FileText, Image, Paperclip, Loader2, File, Search, Play } from 'lucide-react';
import { Tooltip, TooltipContent, TooltipTrigger, TooltipProvider } from '@/components/ui/tooltip';
import { performExport, performImport, downloadCSVTemplate } from '@/lib/exportImport';
import { toast } from 'sonner';
import { RadioGroup, RadioGroupItem } from '@/components/ui/radio-group';
import { Checkbox } from '@/components/ui/checkbox';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import CaseFormPage from '@/components/CaseFormPage';
import PriorityBadge from '@/components/PriorityBadge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter, DialogDescription } from '@/components/ui/dialog';
import { ResizablePanelGroup, ResizablePanel, ResizableHandle } from '@/components/ui/resizable';
import { useQuery, useQueries, useQueryClient } from '@tanstack/react-query';
import {
  useProject, useCreateSuite, useUpdateSuite, useDeleteSuite, useDeleteTestCase,
  useCreateTestRun,
  keys,
} from '@/hooks/useApi';
import { suitesApi, testCasesApi, testStepsApi, attachmentsApi } from '@/lib/api';
import type { LocalCase as TestCase, LocalStep, LocalSuite as Suite } from '@/lib/localTypes';
// TestRun type only needed for legacy Create Run handler shape — removed, using API directly

const UUID_LIKE_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

const buildSuitePrefix = (suiteName: string) => {
  const words = suiteName.match(/[A-Za-z0-9]+/g) ?? [];
  if (words.length === 0) return 'TC';

  const acronymWord = words.find((word) => /^[A-Z0-9]{2,4}$/.test(word));
  if (acronymWord) return acronymWord.slice(0, 4);

  if (words.length > 1) {
    return words.slice(0, 3).map((word) => word[0]).join('').toUpperCase();
  }

  return words[0].slice(0, 3).toUpperCase();
};

const formatCaseDisplayId = ({
  rawId,
  suiteName,
  caseIndex,
  displayId,
  shortId,
}: {
  rawId: string;
  suiteName: string;
  caseIndex: number;
  displayId?: string;
  shortId?: string;
}) => {
  const explicitId = displayId?.trim() || shortId?.trim();
  if (explicitId) return explicitId;
  if (rawId && !UUID_LIKE_REGEX.test(rawId) && rawId.length <= 18) return rawId;
  return `${buildSuitePrefix(suiteName)}-${caseIndex + 1}`;
};

const getCaseDisplayId = (testCase: Pick<TestCase, 'id' | 'displayId'>) => testCase.displayId || testCase.id;

const Repository = () => {
  const { projectId } = useParams<{ projectId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  // ── API data ──────────────────────────────────────────────────────────────
  const { data: project } = useProject(projectId!);

  // 1. Fetch suites list
  const suitesQuery = useQueries({
    queries: [{ queryKey: keys.suites.all(projectId!), queryFn: () => suitesApi.list(projectId!), enabled: !!projectId, select: (r: { data: { id: string; projectId: string; name: string; description?: string }[] }) => r.data }],
  });
  const apiSuitesData = suitesQuery[0]?.data ?? [];
  const suitesLoading2 = suitesQuery[0]?.isLoading ?? true;

  // 2. Fetch cases for each suite in parallel
  const caseQueries = useQueries({
    queries: apiSuitesData.map(suite => ({
      queryKey: keys.cases.all(projectId!, suite.id),
      queryFn:  () => testCasesApi.list(projectId!, suite.id),
      enabled:  !!projectId && apiSuitesData.length > 0,
      select:   (r: { data: { id: string; displayId?: string; shortId?: string; suiteId: string; title: string; priority: 'HIGH' | 'MEDIUM' | 'LOW'; description: string; preconditions: string; deleted: boolean }[] }) => r.data,
    })),
  });

  // Build the nested Suite[] that the UI expects (same shape as old mockData)
  const suites: Suite[] = useMemo(() => {
    return apiSuitesData.map((s, i) => ({
      id:        s.id,
      projectId: s.projectId,
      name:      s.name,
      cases:     (caseQueries[i]?.data ?? []).map((c, caseIndex) => ({
        id:            c.id,
        displayId:     formatCaseDisplayId({
          rawId: c.id,
          suiteName: s.name,
          caseIndex,
          displayId: c.displayId,
          shortId: c.shortId,
        }),
        suiteId:       c.suiteId,
        title:         c.title,
        priority:      c.priority,
        description:   c.description,
        preconditions: c.preconditions,
        steps:         [],    // loaded lazily when a case is selected
        deleted:       c.deleted,
      })),
    }));
  }, [apiSuitesData, caseQueries]);

  const isLoadingRepo = suitesLoading2 || caseQueries.some(q => q.isLoading && !q.data);

  const [selectedCase, setSelectedCase] = useState<TestCase | null>(null);

  // Expand all suites once they first load
  const [expandedSuites, setExpandedSuites] = useState<Set<string>>(new Set());
  useEffect(() => {
    if (apiSuitesData.length > 0) {
      setExpandedSuites(prev => {
        if (prev.size === 0) return new Set(apiSuitesData.map(s => s.id));
        return prev;
      });
    }
  }, [apiSuitesData]);

  // Auto-select first case only once when data first loads
  const autoSelectedRef = useRef(false);
  useEffect(() => {
    if (!autoSelectedRef.current && suites.length > 0) {
      const firstCase = suites[0]?.cases?.[0];
      if (firstCase) {
        autoSelectedRef.current = true;
        setSelectedCase(firstCase);
      }
    }
  }, [suites]);

  // 3. Fetch steps for the selected case (lazy)
  const selectedCaseSuiteId = useMemo(
    () => suites.find(s => s.cases.some(c => c.id === selectedCase?.id))?.id ?? '',
    [suites, selectedCase?.id]
  );
  const stepsQuery = useQueries({
    queries: selectedCase && selectedCaseSuiteId
      ? [{
          queryKey: keys.steps.all(projectId!, selectedCaseSuiteId, selectedCase.id),
          queryFn:  () => testStepsApi.list(projectId!, selectedCaseSuiteId, selectedCase.id),
          // backend returns a plain array (not { data: [...] })
          select:   (r: { id: string; stepNumber: number; action: string; expectedResult: string; status: string }[]) => r,
        }]
      : [],
  });
  const stepsForSelectedCase: LocalStep[] = useMemo(
    () => (stepsQuery[0]?.data ?? []).map(s => ({
      id:             s.id,
      order:          s.stepNumber,
      action:         s.action,
      expectedResult: s.expectedResult,
      status:         s.status,
    })),
    [stepsQuery[0]?.data]
  );

  // 4. Fetch attachments for the selected case (lazy)
  const { data: caseAttachments = [] } = useQuery({
    queryKey: ['attachments', projectId, selectedCase?.id],
    queryFn:  () => attachmentsApi.listByCase(projectId!, selectedCase!.id),
    enabled:  !!projectId && !!selectedCase?.id,
  });

  // ── Mutations ─────────────────────────────────────────────────────────────
  const createSuiteMut = useCreateSuite();
  const updateSuiteMut = useUpdateSuite();
  const deleteSuiteMut = useDeleteSuite();
  const deleteCaseMut = useDeleteTestCase();
  const createRunMut   = useCreateTestRun();

  // Fixed panel sizes
  const panelSizes = { left: 15, middle: 50, right: 35 };

  // Local search
  const [localSearch, setLocalSearch] = useState('');

  // Bulk selection
  const [selectedCases, setSelectedCases] = useState<Set<string>>(new Set());

  // Bulk delete confirmation
  const [bulkDeleteOpen, setBulkDeleteOpen] = useState(false);

  // Create Test Run modal
  const [createRunOpen, setCreateRunOpen] = useState(false);
  const [newRunName, setNewRunName] = useState('');
  const [newRunEnv, setNewRunEnv] = useState('Staging');
  const [newRunBuildVersion, setNewRunBuildVersion] = useState('');
  const [newRunDesc, setNewRunDesc] = useState('');

  // Stats
  const totalCases = suites.reduce((sum, s) => sum + s.cases.length, 0);
  const totalSuites = suites.length;

  // Filtered suites for local search
  const filteredSuites = useMemo(() => {
    const q = localSearch.trim().toLowerCase();
    if (!q) return suites;
    return suites
      .map((suite) => {
        const suiteMatch = suite.name.toLowerCase().includes(q);
        const filteredCases = suite.cases.filter(
          (c) =>
            c.title.toLowerCase().includes(q) ||
            (c.displayId ?? '').toLowerCase().includes(q) ||
            c.id.toLowerCase().includes(q) ||
            c.description.toLowerCase().includes(q)
        );
        if (suiteMatch || filteredCases.length > 0) {
          return { ...suite, cases: suiteMatch ? suite.cases : filteredCases };
        }
        return null;
      })
      .filter(Boolean) as Suite[];
  }, [suites, localSearch]);

  // Highlight helper
  const HighlightText = ({ text, query }: { text: string; query: string }) => {
    if (!query.trim()) return <>{text}</>;
    const regex = new RegExp(`(${query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi');
    const parts = text.split(regex);
    return (
      <>
        {parts.map((part, i) =>
          regex.test(part) ? (
            <span key={i} className="text-primary font-semibold">{part}</span>
          ) : (
            <span key={i}>{part}</span>
          )
        )}
      </>
    );
  };

  // Bulk selection helpers
  const toggleCaseSelection = (caseId: string) => {
    setSelectedCases((prev) => {
      const next = new Set(prev);
      next.has(caseId) ? next.delete(caseId) : next.add(caseId);
      return next;
    });
  };

  const toggleSuiteSelection = (suite: Suite) => {
    const allSelected = suite.cases.every((c) => selectedCases.has(c.id));
    setSelectedCases((prev) => {
      const next = new Set(prev);
      suite.cases.forEach((c) => {
        allSelected ? next.delete(c.id) : next.add(c.id);
      });
      return next;
    });
  };

  // Toggle all cases across all suites
  const toggleSelectAll = () => {
    const allCasesInAllSuites = suites.flatMap((s) => s.cases);
    const allCasesSelected = allCasesInAllSuites.every((c) => selectedCases.has(c.id));
    if (allCasesSelected) {
      setSelectedCases(new Set());
    } else {
      setSelectedCases(new Set(allCasesInAllSuites.map((c) => c.id)));
    }
  };

  // Check if all cases are selected
  const allCasesSelected = (() => {
    const allCasesInAllSuites = suites.flatMap((s) => s.cases);
    return allCasesInAllSuites.length > 0 && allCasesInAllSuites.every((c) => selectedCases.has(c.id));
  })();

  const clearSelection = () => setSelectedCases(new Set());

  const handleBulkDelete = async () => {
    if (!projectId) return;
    const toDelete = [...selectedCases];
    try {
      await Promise.all(toDelete.map(caseId => {
        const suite = suites.find(s => s.cases.some(c => c.id === caseId));
        if (!suite) return Promise.resolve();
        return testCasesApi.delete(projectId, suite.id, caseId);
      }));
      // Invalidate all case queries
      suites.forEach(s => queryClient.invalidateQueries({ queryKey: keys.cases.all(projectId, s.id) }));
      if (selectedCase && selectedCases.has(selectedCase.id)) setSelectedCase(null);
      setSelectedCases(new Set());
      setBulkDeleteOpen(false);
      toast.success(`Deleted ${toDelete.length} case(s)`);
    } catch (e) {
      toast.error(e instanceof Error ? e.message : 'Failed to delete cases');
    }
  };

  const handleBulkCopy = async () => {
    if (!projectId) return;
    try {
      await Promise.all([...selectedCases].map(caseId => {
        const suite = suites.find(s => s.cases.some(c => c.id === caseId));
        const tc = suite?.cases.find(c => c.id === caseId);
        if (!suite || !tc) return Promise.resolve();
        return testCasesApi.create(projectId, suite.id, {
          title: `${tc.title} (Copy)`, priority: tc.priority,
          description: tc.description, preconditions: tc.preconditions,
        });
      }));
      suites.forEach(s => queryClient.invalidateQueries({ queryKey: keys.cases.all(projectId, s.id) }));
      toast.success(`Duplicated ${selectedCases.size} case(s)`);
      clearSelection();
    } catch (e) {
      toast.error(e instanceof Error ? e.message : 'Failed to duplicate cases');
    }
  };
  // Suite modal
  const [suiteModalOpen, setSuiteModalOpen] = useState(false);
  const [suiteModalMode, setSuiteModalMode] = useState<'create' | 'edit'>('create');
  const [editingSuiteId, setEditingSuiteId] = useState<string>('');
  const [newSuiteName, setNewSuiteName] = useState('');

  // Case form (full-page)
  const [caseFormOpen, setCaseFormOpen] = useState(false);
  const [caseModalMode, setCaseModalMode] = useState<'create' | 'edit'>('create');
  const [editingCaseId, setEditingCaseId] = useState<string>('');
  const [caseSuiteId, setCaseSuiteId] = useState<string>('');
  const [editingCase, setEditingCase] = useState<TestCase | null>(null);

  // Quick create modal
  const [quickCreateOpen, setQuickCreateOpen] = useState(false);
  const [quickCreateSuiteId, setQuickCreateSuiteId] = useState<string>('');
  const [quickCreateTitle, setQuickCreateTitle] = useState('');

  // Delete confirmation
  const [deleteModalOpen, setDeleteModalOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<{ type: 'suite' | 'case'; id: string; name: string } | null>(null);

  // Attachment preview modal
  const [previewAttachment, setPreviewAttachment] = useState<{ name: string; url: string; type: string } | null>(null);

  // Export modal
  const [exportOpen, setExportOpen] = useState(false);
  const [exportScope, setExportScope] = useState<'all' | 'selected'>('all');
  const [exportFormat, setExportFormat] = useState('csv');
  const [exportSelectedSuites, setExportSelectedSuites] = useState<Set<string>>(new Set());
  const [exportSelectedCases, setExportSelectedCases] = useState<Set<string>>(new Set());
  const [exportExpandedSuites, setExportExpandedSuites] = useState<Set<string>>(new Set());
  const [exportIncludeSteps, setExportIncludeSteps] = useState(true);
  const [exportIncludePreconditions, setExportIncludePreconditions] = useState(true);
  const [exporting, setExporting] = useState(false);

  // Import modal
  const [importOpen, setImportOpen] = useState(false);
  const [importFile, setImportFile] = useState<File | null>(null);
  const [importSuiteTarget, setImportSuiteTarget] = useState('root');
  const [importConflict, setImportConflict] = useState<'skip' | 'overwrite' | 'create_new'>('skip');
  const [importDragOver, setImportDragOver] = useState(false);
  const [importing, setImporting] = useState(false);

  const toggleExportSuite = (id: string) => {
    const suite = suites.find(s => s.id === id);
    setExportSelectedSuites((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
    if (suite) {
      setExportSelectedCases((prev) => {
        const next = new Set(prev);
        const allSelected = suite.cases.every(c => prev.has(c.id));
        suite.cases.forEach(c => allSelected ? next.delete(c.id) : next.add(c.id));
        return next;
      });
    }
  };

  const toggleExportCase = (caseId: string, suiteId: string) => {
    setExportSelectedCases((prev) => {
      const next = new Set(prev);
      next.has(caseId) ? next.delete(caseId) : next.add(caseId);
      // Auto-check/uncheck suite
      const suite = suites.find(s => s.id === suiteId);
      if (suite) {
        const allSelected = suite.cases.every(c => (c.id === caseId ? !prev.has(caseId) : next.has(c.id)));
        const nextSuites = new Set(exportSelectedSuites);
        allSelected ? nextSuites.add(suiteId) : nextSuites.delete(suiteId);
        setExportSelectedSuites(nextSuites);
      }
      return next;
    });
  };

  const toggleExportSuiteExpand = (id: string) => {
    setExportExpandedSuites(prev => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  };

  const handleExport = async () => {
    setExporting(true);
    try {
      const targetSuites = exportScope === 'selected'
        ? suites
            .filter(s => s.cases.some(c => exportSelectedCases.has(c.id)))
            .map(s => ({ ...s, cases: s.cases.filter(c => exportSelectedCases.has(c.id)) }))
        : suites;

      const suitesForExport = exportIncludeSteps
        ? await Promise.all(targetSuites.map(async (suite) => ({
            ...suite,
            cases: await Promise.all(suite.cases.map(async (tc) => {
              const steps = await queryClient.fetchQuery({
                queryKey: keys.steps.all(projectId!, suite.id, tc.id),
                queryFn: async () => {
                  const data = await testStepsApi.list(projectId!, suite.id, tc.id);
                  return data.map(step => ({
                    id: step.id,
                    order: step.stepNumber,
                    action: step.action,
                    expectedResult: step.expectedResult,
                    status: step.status,
                  }));
                },
              });

              return { ...tc, steps };
            })),
          })))
        : targetSuites;

      await performExport({
        suites: suitesForExport,
        projectName: project?.name || 'Project',
        format: exportFormat,
        includeSteps: exportIncludeSteps,
        includePreconditions: exportIncludePreconditions,
      });
      toast.success('Export completed successfully');
    } catch (err) {
      toast.error('Export failed: ' + (err instanceof Error ? err.message : 'Unknown error'));
    } finally {
      setExporting(false);
      setExportOpen(false);
    }
  };

  const handleImportDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setImportDragOver(false);
    const file = e.dataTransfer.files?.[0];
    if (file && /\.(csv|json|xml)$/i.test(file.name)) setImportFile(file);
  }, []);

  const handleImportFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) setImportFile(file);
  };

  const handleImport = async () => {
    if (!importFile || !projectId) return;
    setImporting(true);
    try {
      const targetId = importSuiteTarget === 'root' ? '__new__' : importSuiteTarget;
      const { updatedSuites, result } = await performImport(importFile, targetId, importConflict, suites, projectId);
      const affectedSuiteIds = new Set<string>();
      // Persist parsed cases to the backend
      for (const suite of updatedSuites) {
        const existing = suites.find(s => s.id === suite.id);
        const newCases = existing
          ? suite.cases.filter(c => !existing.cases.some(ec => ec.id === c.id))
          : suite.cases;
        if (newCases.length === 0) continue;
        let suiteId = suite.id;
        if (!existing) {
          const created = await suitesApi.create(projectId, { name: suite.name });
          suiteId = created.id;
        }
        affectedSuiteIds.add(suiteId);
        for (const tc of newCases) {
          const created = await testCasesApi.create(projectId, suiteId, {
            title: tc.title, priority: tc.priority,
            description: tc.description, preconditions: tc.preconditions,
          });
          // Create steps for the case
          for (const step of tc.steps || []) {
            await testStepsApi.create(projectId, suiteId, created.id, {
              stepNumber: step.order || 1,
              action: step.action,
              expectedResult: step.expectedResult,
            });
          }
        }
      }
      // Refetch suites and affected case lists so imported cases become visible immediately
      await queryClient.refetchQueries({ queryKey: keys.suites.all(projectId) });
      await Promise.all(
        [...affectedSuiteIds].map((suiteId) =>
          queryClient.refetchQueries({ queryKey: keys.cases.all(projectId, suiteId) })
        )
      );
      const parts: string[] = [];
      if (result.imported > 0) parts.push(`${result.imported} imported`);
      if (result.overwritten > 0) parts.push(`${result.overwritten} overwritten`);
      if (result.skipped > 0) parts.push(`${result.skipped} skipped`);
      toast.success(`Import complete: ${parts.join(', ')}`);
      setExpandedSuites((prev) => new Set([...prev, ...affectedSuiteIds]));
    } catch (err) {
      toast.error('Import failed: ' + (err instanceof Error ? err.message : 'Invalid file format'));
    } finally {
      setImporting(false);
      setImportOpen(false);
      setImportFile(null);
    }
  };

  const toggleSuite = (id: string) => {
    const next = new Set(expandedSuites);
    next.has(id) ? next.delete(id) : next.add(id);
    setExpandedSuites(next);
  };

  // ── Suite CRUD ──
  const openCreateSuite = () => {
    setSuiteModalMode('create');
    setNewSuiteName('');
    setEditingSuiteId('');
    setSuiteModalOpen(true);
  };

  const openEditSuite = (suite: Suite) => {
    setSuiteModalMode('edit');
    setNewSuiteName(suite.name);
    setEditingSuiteId(suite.id);
    setSuiteModalOpen(true);
  };

  const handleSaveSuite = () => {
    if (!newSuiteName.trim() || !projectId) return;
    if (suiteModalMode === 'create') {
      createSuiteMut.mutate(
        { projectId, body: { name: newSuiteName.trim() } },
        {
          onSuccess: (s) => {
            setExpandedSuites((prev) => new Set([...prev, s.id]));
            setSuiteModalOpen(false);
            toast.success('Suite created');
          },
          onError: (e) => toast.error(e.message),
        }
      );
    } else {
      updateSuiteMut.mutate(
        { projectId, id: editingSuiteId, body: { name: newSuiteName.trim() } },
        {
          onSuccess: () => { setSuiteModalOpen(false); toast.success('Suite updated'); },
          onError: (e) => toast.error(e.message),
        }
      );
    }
  };

  const copySuite = async (suite: Suite) => {
    if (!projectId) return;
    try {
      const newSuite = await suitesApi.create(projectId, {
        name: `${suite.name} (Copy)`,
      });
      for (const tc of suite.cases) {
        await testCasesApi.create(projectId, newSuite.id, {
          title: tc.title,
          priority: tc.priority,
          description: tc.description || '',
          preconditions: tc.preconditions || '',
        });
      }
      queryClient.invalidateQueries({ queryKey: keys.suites.all(projectId) });
      setExpandedSuites((prev) => new Set([...prev, newSuite.id]));
      toast.success(`Suite "${suite.name}" duplicated`);
    } catch (e) {
      toast.error(e instanceof Error ? e.message : 'Failed to copy suite');
    }
  };

  const confirmDeleteSuite = (suite: Suite) => {
    setDeleteTarget({ type: 'suite', id: suite.id, name: suite.name });
    setDeleteModalOpen(true);
  };

  // ── Case CRUD ──
  const openCreateCase = (suiteId?: string) => {
    setCaseModalMode('create');
    setCaseSuiteId(suiteId || (suites.length > 0 ? suites[0].id : ''));
    setEditingCaseId('');
    setEditingCase(null);
    setCaseFormOpen(true);
  };

  const openEditCase = (tc: TestCase) => {
    setCaseModalMode('edit');
    setEditingCaseId(tc.id);
    setCaseSuiteId(tc.suiteId);
    setEditingCase(tc);
    setCaseFormOpen(true);
  };

  const handleSaveCase = async (
    targetSuiteId: string,
    data: { title: string; priority: 'HIGH' | 'MEDIUM' | 'LOW'; description: string; preconditions: string; steps: LocalStep[]; files: File[] }
  ) => {
    if (!projectId) return;
    try {
      if (caseModalMode === 'create') {
        const newCase = await testCasesApi.create(projectId, targetSuiteId, {
          title: data.title, priority: data.priority,
          description: data.description, preconditions: data.preconditions,
        });
        for (const step of data.steps) {
          await testStepsApi.create(projectId, targetSuiteId, newCase.id, {
            stepNumber: step.order, action: step.action, expectedResult: step.expectedResult,
          });
        }
        for (const file of data.files) {
          await attachmentsApi.upload(projectId, file, newCase.id);
        }
        queryClient.invalidateQueries({ queryKey: keys.cases.all(projectId, targetSuiteId) });
        queryClient.invalidateQueries({ queryKey: ['attachments', projectId, newCase.id] });
        toast.success('Test case created');
      } else {
        await testCasesApi.update(projectId, caseSuiteId, editingCaseId, {
          title: data.title, priority: data.priority,
          description: data.description, preconditions: data.preconditions,
        });
        // Replace all steps: delete existing then recreate
        for (const step of stepsForSelectedCase) {
          if (step.id) await testStepsApi.delete(projectId, caseSuiteId, editingCaseId, step.id);
        }
        for (const step of data.steps) {
          await testStepsApi.create(projectId, caseSuiteId, editingCaseId, {
            stepNumber: step.order, action: step.action, expectedResult: step.expectedResult,
          });
        }
        for (const file of data.files) {
          await attachmentsApi.upload(projectId, file, editingCaseId);
        }
        queryClient.invalidateQueries({ queryKey: keys.cases.all(projectId, caseSuiteId) });
        queryClient.invalidateQueries({ queryKey: keys.steps.all(projectId, caseSuiteId, editingCaseId) });
        queryClient.invalidateQueries({ queryKey: ['attachments', projectId, editingCaseId] });
        if (selectedCase?.id === editingCaseId) {
          setSelectedCase({ ...selectedCase, title: data.title, priority: data.priority, description: data.description, preconditions: data.preconditions, suiteId: targetSuiteId });
        }
        toast.success('Test case updated');
      }
      setCaseFormOpen(false);
    } catch (e) {
      toast.error(e instanceof Error ? e.message : 'Failed to save test case');
    }
  };

  const copyCase = async (tc: TestCase) => {
    if (!projectId) return;
    try {
      await testCasesApi.create(projectId, tc.suiteId, {
        title: `${tc.title} (Copy)`, priority: tc.priority,
        description: tc.description, preconditions: tc.preconditions,
      });
      queryClient.invalidateQueries({ queryKey: keys.cases.all(projectId, tc.suiteId) });
      toast.success('Test case duplicated');
    } catch (e) {
      toast.error(e instanceof Error ? e.message : 'Failed to duplicate test case');
    }
  };

  const confirmDeleteCase = (tc: TestCase) => {
    setDeleteTarget({ type: 'case', id: tc.id, name: tc.title });
    setDeleteModalOpen(true);
  };

  const handleDelete = () => {
    if (!deleteTarget || !projectId) return;
    if (deleteTarget.type === 'suite') {
      deleteSuiteMut.mutate(
        { projectId, id: deleteTarget.id },
        {
          onSuccess: () => {
            if (selectedCase && suites.find(s => s.id === deleteTarget.id)?.cases.some(c => c.id === selectedCase.id)) {
              setSelectedCase(null);
            }
            setDeleteModalOpen(false);
            setDeleteTarget(null);
            toast.success('Suite deleted');
          },
          onError: (e) => toast.error(e.message),
        }
      );
    } else {
      const suiteForCase = suites.find(s => s.cases.some(c => c.id === deleteTarget.id));
      if (!suiteForCase) return;
      deleteCaseMut.mutate(
        { projectId, suiteId: suiteForCase.id, id: deleteTarget.id },
        {
          onSuccess: () => {
            if (selectedCase?.id === deleteTarget.id) setSelectedCase(null);
            setDeleteModalOpen(false);
            setDeleteTarget(null);
            toast.success('Test case deleted');
          },
          onError: (e) => toast.error(e.message),
        }
      );
    }
  };

  // When editing a case, merge fetched steps in so CaseFormPage can pre-populate them
  const editingCaseWithSteps: TestCase | null = editingCase
    ? { ...editingCase, steps: stepsForSelectedCase }
    : null;

  if (caseFormOpen) {
    return (
      <div className="h-full">
        <CaseFormPage
          mode={caseModalMode}
          suites={suites}
          initialSuiteId={caseSuiteId}
          initialCase={editingCaseWithSteps || undefined}
          projectName={project?.name}
          projectId={projectId}
          onSave={handleSaveCase}
          onCancel={() => setCaseFormOpen(false)}
        />
      </div>
    );
  }

  if (isLoadingRepo) {
    return (
      <div className="h-full flex flex-col">
        <div className="px-6 pt-5 pb-4 bg-card">
          <div className="mb-2 h-4 w-40 bg-muted animate-pulse rounded" />
          <div className="h-6 w-32 bg-muted animate-pulse rounded" />
        </div>
        <div className="flex-1 flex items-center justify-center text-muted-foreground gap-2">
          <Loader2 className="h-5 w-5 animate-spin" />
          <span>Loading repository…</span>
        </div>
      </div>
    );
  }

  return (
    <div className="h-full flex flex-col">
      {/* Breadcrumbs & Actions */}
      <div className="px-6 pt-5 pb-4 bg-card">
        <div className="mb-2">
          <Breadcrumbs segments={[
            { label: 'Projects', href: '/projects' },
            { label: project?.name || 'Project', href: `/projects/${projectId}/repository` },
            { label: 'Repository' },
          ]} />
        </div>
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-xl font-bold text-foreground tracking-[-0.02em]">Repository</h1>
            <p className="text-xs text-muted-foreground mt-0.5">{totalCases} cases · {totalSuites} suites</p>
          </div>
          <div className="flex items-center gap-2">
            <Button variant="ghost" size="sm" className="gap-1.5 text-muted-foreground" onClick={() => setExportOpen(true)}>
              <Upload className="h-3.5 w-3.5" strokeWidth={1.5} /> Export
            </Button>
            <Button variant="ghost" size="sm" className="gap-1.5 text-muted-foreground" onClick={() => { setImportFile(null); setImportOpen(true); }}>
              <Download className="h-3.5 w-3.5" strokeWidth={1.5} /> Import
            </Button>
            <Button
              size="sm"
              className="bg-gradient-to-br from-primary to-primary/80 text-primary-foreground gap-1.5 border-0"
              onClick={() => openCreateCase()}
            >
              <Plus className="h-3.5 w-3.5" strokeWidth={1.5} /> Case
            </Button>
          </div>
        </div>
      </div>

      {/* Bulk Action Toolbar */}
      {selectedCases.size > 0 && (
        <div className="px-6 py-2 bg-primary/5 border-b border-primary/10 flex items-center gap-3">
          <span className="text-sm font-medium text-foreground">Selected: {selectedCases.size} case{selectedCases.size !== 1 ? 's' : ''}</span>
          <button onClick={clearSelection} className="text-muted-foreground hover:text-foreground transition-colors">
            <X className="h-3.5 w-3.5" strokeWidth={1.5} />
          </button>
          <div className="ml-auto flex items-center gap-1">
            <TooltipProvider delayDuration={200}>
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button variant="ghost" size="icon" className="h-8 w-8 text-muted-foreground hover:text-foreground" onClick={() => {
                    setExportScope('selected');
                    setExportSelectedCases(new Set(selectedCases));
                    setExportSelectedSuites(new Set(suites.filter(s => s.cases.every(c => selectedCases.has(c.id))).map(s => s.id)));
                    setExportExpandedSuites(new Set(suites.filter(s => s.cases.some(c => selectedCases.has(c.id))).map(s => s.id)));
                    setExportOpen(true);
                  }}>
                    <Download className="h-4 w-4" strokeWidth={1.5} />
                  </Button>
                </TooltipTrigger>
                <TooltipContent>Export selected</TooltipContent>
              </Tooltip>
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button variant="ghost" size="icon" className="h-8 w-8 text-muted-foreground hover:text-foreground" onClick={handleBulkCopy}>
                    <Copy className="h-4 w-4" strokeWidth={1.5} />
                  </Button>
                </TooltipTrigger>
                <TooltipContent>Duplicate selected</TooltipContent>
              </Tooltip>
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button variant="ghost" size="icon" className="h-8 w-8 text-muted-foreground hover:text-foreground" onClick={() => {
                    setNewRunName('');
                    setNewRunEnv('Staging');
                    setNewRunBuildVersion('');
                    setNewRunDesc('');
                    setCreateRunOpen(true);
                  }}>
                    <Play className="h-4 w-4" strokeWidth={1.5} />
                  </Button>
                </TooltipTrigger>
                <TooltipContent>Run selected</TooltipContent>
              </Tooltip>
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button variant="ghost" size="icon" className="h-8 w-8 text-muted-foreground hover:text-destructive" onClick={() => setBulkDeleteOpen(true)}>
                    <Trash2 className="h-4 w-4" strokeWidth={1.5} />
                  </Button>
                </TooltipTrigger>
                <TooltipContent>Delete selected</TooltipContent>
              </Tooltip>
            </TooltipProvider>
          </div>
        </div>
      )}

      {/* Three-pane resizable layout */}
      <div className="flex-1 overflow-hidden">
        <ResizablePanelGroup
          direction="horizontal"
          className="h-full"
        >
          {/* Suite Tree */}
          <ResizablePanel id="left" order={1} defaultSize={panelSizes.left} minSize={10} maxSize={30}>
            <div className="h-full surface-low overflow-auto">
              <div className="flex items-center justify-between p-3 group">
                <div className="flex items-center gap-2">
                  <div className={`flex items-center justify-center transition-opacity ${selectedCases.size > 0 ? 'opacity-100' : 'opacity-0 group-hover:opacity-100'}`}>
                    <Checkbox
                      checked={allCasesSelected}
                      onCheckedChange={toggleSelectAll}
                      className="h-3.5 w-3.5"
                    />
                  </div>
                  <span className="text-xs font-semibold text-foreground uppercase tracking-wider">Suites</span>
                </div>
                <Button variant="ghost" size="icon" className="h-6 w-6" onClick={openCreateSuite}>
                  <Plus className="h-3.5 w-3.5" strokeWidth={1.5} />
                </Button>
              </div>
              <div className="px-2 pb-2">
                {suites.map((suite) => (
                  <div
                    key={suite.id}
                    className="w-full flex items-center gap-2 px-2 py-2 rounded-md text-sm hover:bg-card transition-colors"
                  >
                    <button
                      onClick={(e) => { e.stopPropagation(); toggleSuite(suite.id); }}
                      className="shrink-0 p-0.5 rounded hover:bg-muted transition-colors"
                      aria-label={expandedSuites.has(suite.id) ? 'Collapse suite' : 'Expand suite'}
                    >
                      {expandedSuites.has(suite.id) ? <ChevronDown className="h-3.5 w-3.5 text-muted-foreground" strokeWidth={1.5} /> : <ChevronRight className="h-3.5 w-3.5 text-muted-foreground" strokeWidth={1.5} />}
                    </button>
                    <button
                      onClick={() => {
                        const el = document.getElementById(`suite-section-${suite.id}`);
                        el?.scrollIntoView({ behavior: 'smooth', block: 'start' });
                      }}
                      className="flex-1 text-left truncate text-foreground hover:text-primary transition-colors"
                    >
                      {suite.name}
                    </button>
                    <span className="text-[10px] text-muted-foreground font-mono">{suite.cases.length}</span>
                  </div>
                ))}
              </div>
            </div>
          </ResizablePanel>

          <ResizableHandle withHandle />

          {/* Case List */}
          <ResizablePanel id="middle" order={2} defaultSize={selectedCase ? panelSizes.middle : 100 - panelSizes.left} minSize={30}>
            <div className="h-full min-w-0 overflow-auto bg-card">
              {filteredSuites.map((suite) => (
                <div key={suite.id} id={`suite-section-${suite.id}`} className="border-b border-border/40 last:border-b-0">
                  <div className="group flex items-center justify-between px-5 py-2.5 surface-low">
                    <div className="flex items-center gap-2">
                      <div className={`flex items-center justify-center transition-opacity ${selectedCases.size > 0 ? 'opacity-100' : 'opacity-0 group-hover:opacity-100'}`}>
                        <Checkbox
                          checked={suite.cases.length > 0 && suite.cases.every((c) => selectedCases.has(c.id))}
                          onCheckedChange={() => toggleSuiteSelection(suite)}
                          className="h-3.5 w-3.5"
                        />
                      </div>
                      <span className="text-sm font-semibold text-foreground"><HighlightText text={suite.name} query={localSearch} /></span>
                    </div>
                    <div className="flex items-center gap-1">
                      <Button variant="ghost" size="icon" className="h-7 w-7 text-muted-foreground" onClick={() => openCreateCase(suite.id)}>
                        <Plus className="h-3.5 w-3.5" strokeWidth={1.5} />
                      </Button>
                      <Button variant="ghost" size="icon" className="h-7 w-7 text-muted-foreground" onClick={() => openEditSuite(suite)}>
                        <Pencil className="h-3.5 w-3.5" strokeWidth={1.5} />
                      </Button>
                      <Button variant="ghost" size="icon" className="h-7 w-7 text-muted-foreground" onClick={() => copySuite(suite)}>
                        <Copy className="h-3.5 w-3.5" strokeWidth={1.5} />
                      </Button>
                      <Button variant="ghost" size="icon" className="h-7 w-7 text-muted-foreground hover:text-destructive" onClick={() => confirmDeleteSuite(suite)}>
                        <Trash2 className="h-3.5 w-3.5" strokeWidth={1.5} />
                      </Button>
                    </div>
                  </div>

                  {expandedSuites.has(suite.id) && (
                    <div className="py-1">
                      {suite.cases.map((tc) => (
                        <div
                          key={tc.id}
                          onClick={() => setSelectedCase(tc)}
                          className={`group grid grid-cols-[16px_minmax(60px,88px)_minmax(0,1fr)_auto_auto] items-center gap-3 px-5 py-2.5 mx-2 rounded cursor-pointer transition-colors ${selectedCases.has(tc.id) ? 'bg-indigo-50' : selectedCase?.id === tc.id ? 'surface-high' : 'hover:surface-low'}`}
                        >
                          <div className={`flex items-center justify-center transition-opacity ${selectedCases.has(tc.id) || selectedCases.size > 0 ? 'opacity-100' : 'opacity-0 group-hover:opacity-100'}`}>
                            <Checkbox
                              checked={selectedCases.has(tc.id)}
                              onCheckedChange={() => toggleCaseSelection(tc.id)}
                              className="h-3.5 w-3.5"
                              onClick={(e) => e.stopPropagation()}
                            />
                          </div>
                          <span
                            className="min-w-[60px] max-w-[88px] overflow-hidden text-ellipsis whitespace-nowrap text-[10px] font-mono tracking-wider text-muted-foreground"
                            title={tc.id}
                          >
                            <HighlightText text={getCaseDisplayId(tc)} query={localSearch} />
                          </span>
                          <span className="min-w-0 text-sm font-medium text-foreground truncate">
                            <HighlightText text={tc.title} query={localSearch} />
                          </span>
                          <PriorityBadge priority={tc.priority} className="ml-auto shrink-0 justify-self-end" />
                          <div className="flex items-center gap-0.5 justify-self-end shrink-0">
                            <Button variant="ghost" size="icon" className="h-6 w-6 text-muted-foreground" onClick={(e) => { e.stopPropagation(); openEditCase(tc); }}>
                              <Pencil className="h-3 w-3" strokeWidth={1.5} />
                            </Button>
                            <Button variant="ghost" size="icon" className="h-6 w-6 text-muted-foreground" onClick={(e) => { e.stopPropagation(); copyCase(tc); }}>
                              <Copy className="h-3 w-3" strokeWidth={1.5} />
                            </Button>
                            <Button variant="ghost" size="icon" className="h-6 w-6 text-muted-foreground hover:text-destructive" onClick={(e) => { e.stopPropagation(); confirmDeleteCase(tc); }}>
                              <Trash2 className="h-3 w-3" strokeWidth={1.5} />
                            </Button>
                          </div>
                        </div>
                      ))}
                      <div className="px-5 py-2">
                        <Button variant="ghost" size="sm" className="text-xs text-muted-foreground gap-1" onClick={() => { setQuickCreateSuiteId(suite.id); setQuickCreateTitle(''); setQuickCreateOpen(true); }}>
                          <Plus className="h-3 w-3" strokeWidth={1.5} /> Create quick test
                        </Button>
                      </div>
                    </div>
                  )}
                </div>
              ))}
            </div>
          </ResizablePanel>

          {/* Case Detail Panel - Conditionally rendered */}
          {selectedCase && (
            <>
              <ResizableHandle withHandle />
              <ResizablePanel id="right" order={3} defaultSize={panelSizes.right} minSize={20} maxSize={50}>
                <div className="h-full surface-low overflow-auto flex flex-col">
                  <div className="p-4 flex flex-col flex-1 min-h-0 gap-0">
                    {/* Compact Header: Title + metadata + actions in one block */}
                    <div className="flex items-start justify-between gap-2 mb-4">
                      <div className="min-w-0 flex-1">
                        <h2 className="text-sm font-semibold text-foreground truncate mb-1.5 flex items-baseline gap-2">
                          <span className="font-mono text-[10px] text-muted-foreground font-normal shrink-0" title={selectedCase.id}>{getCaseDisplayId(selectedCase)}</span>
                          <span className="truncate">{selectedCase.title}</span>
                        </h2>
                        <div className="flex items-center gap-1.5 text-[10px] font-mono text-muted-foreground">
                          <span className="uppercase tracking-widest">{suites.find((s) => s.id === selectedCase.suiteId)?.name}</span>
                          <span>·</span>
                          <PriorityBadge priority={selectedCase.priority} />
                        </div>
                      </div>
                      <div className="flex items-center gap-0.5 shrink-0">
                        <Button variant="ghost" size="icon" className="h-7 w-7 text-muted-foreground hover:text-foreground" onClick={() => openEditCase(selectedCase)}>
                          <Pencil className="h-3.5 w-3.5" strokeWidth={1.5} />
                        </Button>
                        <Button variant="ghost" size="icon" className="h-7 w-7 text-muted-foreground hover:text-foreground" onClick={() => setSelectedCase(null)}>
                          <X className="h-4 w-4" strokeWidth={1.5} />
                        </Button>
                      </div>
                    </div>

                    {/* Context Block: Description & Pre-conditions */}
                    {(selectedCase.description || selectedCase.preconditions) && (
                      <div className="mb-4 space-y-3">
                        {selectedCase.description && (
                          <div>
                            <label className="text-[10px] font-semibold text-muted-foreground uppercase block font-mono tracking-widest mb-1.5">Description</label>
                            <p className="text-sm text-foreground leading-relaxed">{selectedCase.description}</p>
                          </div>
                        )}
                        {selectedCase.description && selectedCase.preconditions && (
                          <div className="border-t border-border/40" />
                        )}
                        {selectedCase.preconditions && (
                          <div>
                            <label className="text-[10px] font-semibold text-muted-foreground uppercase block font-mono tracking-widest mb-1.5">Pre-conditions</label>
                            <p className="text-sm text-foreground leading-relaxed">{selectedCase.preconditions}</p>
                          </div>
                        )}
                      </div>
                    )}

                    {/* Resources Block: Attachments */}
                    {caseAttachments.length > 0 && (
                      <div className="mb-4">
                        <label className="text-[10px] font-semibold text-muted-foreground uppercase block font-mono tracking-widest mb-1.5">Attachments</label>
                        <div className="flex flex-wrap gap-1.5">
                          {caseAttachments.map((att) => {
                            const isImage = att.fileType?.startsWith('image/');
                            return (
                              <button
                                key={att.id}
                                onClick={() => {
                                  setPreviewAttachment({
                                    name: att.fileName,
                                    url: `/api/projects/${projectId}/attachments/${att.id}/view`,
                                    type: att.fileType || 'application/octet-stream',
                                  });
                                }}
                                className="inline-flex items-center gap-1.5 rounded-full border border-border/60 bg-card px-2.5 py-0.5 cursor-pointer hover:bg-muted/50 transition-colors group"
                              >
                                <FileText className="h-3 w-3 text-muted-foreground shrink-0" strokeWidth={1.5} />
                                <span className="text-[11px] text-foreground truncate max-w-[120px] group-hover:underline">{att.fileName}</span>
                              </button>
                            );
                          })}
                        </div>
                      </div>
                    )}

                    {/* Action Block: Test Steps */}
                    {stepsForSelectedCase.length > 0 && (
                      <div className="flex-1 min-h-0 flex flex-col">
                        <label className="text-[10px] font-semibold text-muted-foreground uppercase block font-mono tracking-widest mb-1.5">Test Steps</label>
                        <div className="rounded-md border border-border/60 bg-card overflow-hidden flex-1">
                          <table className="w-full">
                            <thead>
                              <tr className="bg-muted/50">
                                <th className="px-2.5 py-1.5 text-left text-[9px] font-bold text-muted-foreground uppercase tracking-[0.14em] font-mono w-7">#</th>
                                <th className="px-2.5 py-1.5 text-left text-[9px] font-bold text-muted-foreground uppercase tracking-[0.14em] font-mono">Step</th>
                                <th className="px-2.5 py-1.5 text-left text-[9px] font-bold text-muted-foreground uppercase tracking-[0.14em] font-mono">Expected result</th>
                              </tr>
                            </thead>
                            <tbody>
                              {stepsForSelectedCase.map((step) => (
                                <tr key={step.order} className="ghost-border border-t">
                                  <td className="px-2.5 py-2 text-muted-foreground font-mono text-xs align-baseline">{step.order}</td>
                                  <td className="px-2.5 py-2 text-foreground text-sm leading-relaxed align-baseline">{step.action}</td>
                                  <td className="px-2.5 py-2 text-foreground text-sm leading-relaxed align-baseline">{step.expectedResult}</td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              </ResizablePanel>
            </>
          )}
        </ResizablePanelGroup>
      </div>

      {/* Suite Modal (Create/Edit) */}
      <Dialog open={suiteModalOpen} onOpenChange={setSuiteModalOpen}>
        <DialogContent className="sm:max-w-md glass-surface">
          <DialogHeader>
            <DialogTitle className="text-foreground">{suiteModalMode === 'create' ? 'Create Suite' : 'Edit Suite'}</DialogTitle>
            <DialogDescription className="text-muted-foreground">
              {suiteModalMode === 'create' ? 'Add a new test suite to organize your test cases.' : 'Rename the test suite.'}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-3 py-2">
            <div>
              <label className="text-[10px] font-semibold text-muted-foreground uppercase mb-1.5 block font-mono tracking-widest">Suite Name</label>
              <Input
                placeholder="e.g. Authentication, Checkout Flow"
                value={newSuiteName}
                onChange={(e) => setNewSuiteName(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && handleSaveSuite()}
                autoFocus
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="ghost" size="sm" onClick={() => setSuiteModalOpen(false)}>Cancel</Button>
            <Button size="sm" className="bg-gradient-to-br from-primary to-primary/80 text-primary-foreground border-0" onClick={handleSaveSuite} disabled={!newSuiteName.trim()}>
              {suiteModalMode === 'create' ? 'Create Suite' : 'Save Changes'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation */}
      <Dialog open={deleteModalOpen} onOpenChange={setDeleteModalOpen}>
        <DialogContent className="sm:max-w-sm bg-card">
          <DialogHeader>
            <DialogTitle className="text-foreground flex items-center gap-3">
              <AlertTriangle className="h-6 w-6 text-destructive shrink-0" strokeWidth={1.5} />
              Delete {deleteTarget?.type === 'suite' ? 'Suite' : 'Test Case'}
            </DialogTitle>
            <DialogDescription className="text-sm text-foreground mt-3">
              Are you sure you want to delete <span className="font-bold">{deleteTarget?.name}</span>?
              {deleteTarget?.type === 'suite' && ' All test cases in this suite will also be removed.'}
              {' '}This action cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter className="mt-6">
            <Button variant="outline" size="sm" onClick={() => setDeleteModalOpen(false)}>Cancel</Button>
            <Button size="sm" variant="destructive" onClick={handleDelete}>Delete</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Quick Create Modal */}
      <Dialog open={quickCreateOpen} onOpenChange={setQuickCreateOpen}>
        <DialogContent className="sm:max-w-md glass-surface">
          <DialogHeader>
            <DialogTitle className="text-foreground">Quick Create Test Case</DialogTitle>
            <DialogDescription className="text-muted-foreground">
              Rapidly add a test case with just a title. You can add details later.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-3 py-2">
            <div>
              <label className="text-[10px] font-semibold text-muted-foreground uppercase mb-1.5 block font-mono tracking-widest">Suite</label>
              <Select value={quickCreateSuiteId} onValueChange={setQuickCreateSuiteId}>
                <SelectTrigger className="h-9 bg-white border border-input">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {suites.map((s) => (
                    <SelectItem key={s.id} value={s.id}>{s.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div>
              <label className="text-[10px] font-semibold text-muted-foreground uppercase mb-1.5 block font-mono tracking-widest">Title *</label>
              <Input
                placeholder="e.g. Verify login with valid credentials"
                value={quickCreateTitle}
                onChange={(e) => setQuickCreateTitle(e.target.value)}
                onKeyDown={async (e) => {
                  if (e.key === 'Enter' && quickCreateTitle.trim() && projectId) {
                    try {
                      await testCasesApi.create(projectId, quickCreateSuiteId, {
                        title: quickCreateTitle.trim(), priority: 'MEDIUM',
                        description: '', preconditions: '',
                      });
                      queryClient.invalidateQueries({ queryKey: keys.cases.all(projectId, quickCreateSuiteId) });
                      setQuickCreateOpen(false);
                    } catch (e2) { toast.error(e2 instanceof Error ? e2.message : 'Failed'); }
                  }
                }}
                autoFocus
                className="bg-white"
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="ghost" size="sm" onClick={() => setQuickCreateOpen(false)}>Cancel</Button>
            <Button
              size="sm"
              className="bg-primary text-primary-foreground hover:bg-primary/90 border-0"
              disabled={!quickCreateTitle.trim()}
              onClick={async () => {
                if (!projectId) return;
                try {
                  await testCasesApi.create(projectId, quickCreateSuiteId, {
                    title: quickCreateTitle.trim(), priority: 'MEDIUM',
                    description: '', preconditions: '',
                  });
                  queryClient.invalidateQueries({ queryKey: keys.cases.all(projectId, quickCreateSuiteId) });
                  setQuickCreateOpen(false);
                } catch (e2) { toast.error(e2 instanceof Error ? e2.message : 'Failed'); }
              }}
            >
              Create Case
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Attachment Preview Modal */}
      <Dialog open={!!previewAttachment} onOpenChange={(open) => !open && setPreviewAttachment(null)}>
        <DialogContent className="sm:max-w-2xl bg-white">
          <DialogHeader>
            <DialogTitle className="text-foreground flex items-center gap-2 text-sm">
              <Paperclip className="h-4 w-4 text-muted-foreground" strokeWidth={1.5} />
              {previewAttachment?.name}
            </DialogTitle>
            <DialogDescription className="sr-only">File preview</DialogDescription>
          </DialogHeader>
          <div className="flex items-center justify-center min-h-[300px] rounded-md border border-input bg-muted/30 p-4">
            {previewAttachment?.type?.startsWith('image/') ? (
              <img src={previewAttachment.url} alt={previewAttachment.name} className="max-w-full max-h-[60vh] object-contain rounded" />
            ) : (
              <div className="text-center space-y-3">
                <FileText className="h-16 w-16 text-muted-foreground mx-auto" strokeWidth={1} />
                <p className="text-sm text-muted-foreground">Preview not available for this file type.</p>
                <a href={previewAttachment?.url} target="_blank" rel="noopener noreferrer" className="text-xs text-primary hover:underline">
                  Open in new tab
                </a>
              </div>
            )}
          </div>
          <DialogFooter>
            <Button variant="ghost" onClick={() => setPreviewAttachment(null)}>Close</Button>
            {previewAttachment && (
              <a href={previewAttachment.url} download={previewAttachment.name}>
                <Button className="gap-1.5">
                  <Download className="h-3.5 w-3.5" /> Download
                </Button>
              </a>
            )}
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Export Modal */}
      <Dialog open={exportOpen} onOpenChange={setExportOpen}>
        <DialogContent className="sm:max-w-lg bg-white rounded-xl">
          <DialogHeader>
            <DialogTitle className="text-foreground text-base font-bold">Export Test Cases</DialogTitle>
            <DialogDescription className="text-muted-foreground text-sm">
              Export your test cases to a file for backup or migration.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-5 py-2">
            {/* Export Scope */}
            <div>
              <label className="text-[10px] font-semibold text-muted-foreground uppercase mb-2 block font-mono tracking-widest">Export Scope</label>
              <RadioGroup value={exportScope} onValueChange={(v) => setExportScope(v as 'all' | 'selected')} className="gap-2">
                <div className="flex items-center gap-2">
                  <RadioGroupItem value="all" id="scope-all" />
                  <Label htmlFor="scope-all" className="text-sm text-foreground cursor-pointer">All Repository</Label>
                </div>
                <div className="flex items-center gap-2">
                  <RadioGroupItem value="selected" id="scope-selected" />
                  <Label htmlFor="scope-selected" className="text-sm text-foreground cursor-pointer">Selected Cases</Label>
                </div>
              </RadioGroup>
              {exportScope === 'selected' && (
                <div className="mt-3 ml-6 border-l-2 border-input pl-3">
                  {suites.map((s) => {
                    const isExpanded = exportExpandedSuites.has(s.id);
                    const selectedCount = s.cases.filter(c => exportSelectedCases.has(c.id)).length;
                    return (
                      <div key={s.id}>
                        <div className="flex items-center gap-2 py-1">
                          <button onClick={() => toggleExportSuiteExpand(s.id)} className="shrink-0 p-0.5">
                            {isExpanded ? <ChevronDown className="h-3.5 w-3.5 text-muted-foreground" strokeWidth={1.5} /> : <ChevronRight className="h-3.5 w-3.5 text-muted-foreground" strokeWidth={1.5} />}
                          </button>
                          <Checkbox
                            id={`exp-suite-${s.id}`}
                            checked={s.cases.length > 0 && s.cases.every(c => exportSelectedCases.has(c.id))}
                            onCheckedChange={() => toggleExportSuite(s.id)}
                          />
                          <Label htmlFor={`exp-suite-${s.id}`} className="text-sm text-foreground cursor-pointer">
                            {s.name} <span className="text-muted-foreground text-xs">({selectedCount}/{s.cases.length})</span>
                          </Label>
                        </div>
                        {isExpanded && s.cases.map((c) => (
                          <div key={c.id} className="flex items-center gap-2 py-1 pl-9">
                            <Checkbox
                              id={`exp-case-${c.id}`}
                              checked={exportSelectedCases.has(c.id)}
                              onCheckedChange={() => toggleExportCase(c.id, s.id)}
                            />
                            <Label htmlFor={`exp-case-${c.id}`} className="min-w-0 text-xs text-foreground cursor-pointer truncate">
                              <span className="text-[10px] font-mono text-muted-foreground mr-1.5">{getCaseDisplayId(c)}</span>
                              {c.title}
                            </Label>
                          </div>
                        ))}
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            {/* File Format */}
            <div>
              <label className="text-[10px] font-semibold text-muted-foreground uppercase mb-1.5 block font-mono tracking-widest">File Format</label>
              <Select value={exportFormat} onValueChange={setExportFormat}>
                <SelectTrigger className="bg-white">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="csv">CSV (.csv)</SelectItem>
                  <SelectItem value="json">JSON (.json)</SelectItem>
                  <SelectItem value="xml">XML (.xml)</SelectItem>
                  <SelectItem value="excel">Excel (.xlsx)</SelectItem>
                  <SelectItem value="pdf">PDF (.pdf)</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {/* Data Depth */}
            <div>
              <label className="text-[10px] font-semibold text-muted-foreground uppercase mb-2 block font-mono tracking-widest">Data Depth</label>
              <div className="space-y-2">
                <div className="flex items-center gap-2">
                  <Checkbox id="inc-steps" checked={exportIncludeSteps} onCheckedChange={(v) => setExportIncludeSteps(!!v)} />
                  <Label htmlFor="inc-steps" className="text-sm text-foreground cursor-pointer">Include Test Steps</Label>
                </div>
                <div className="flex items-center gap-2">
                  <Checkbox id="inc-pre" checked={exportIncludePreconditions} onCheckedChange={(v) => setExportIncludePreconditions(!!v)} />
                  <Label htmlFor="inc-pre" className="text-sm text-foreground cursor-pointer">Include Pre-conditions</Label>
                </div>
              </div>
            </div>
          </div>
          <DialogFooter>
            <Button variant="ghost" size="sm" onClick={() => setExportOpen(false)}>Cancel</Button>
            <Button
              size="sm"
              className="bg-primary text-primary-foreground hover:bg-primary/90 border-0 gap-1.5"
              onClick={handleExport}
              disabled={exporting || (exportScope === 'selected' && exportSelectedCases.size === 0)}
            >
              {exporting ? <><Loader2 className="h-3.5 w-3.5 animate-spin" /> Generating...</> : <><Download className="h-3.5 w-3.5" /> Export File</>}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Import Modal */}
      <Dialog open={importOpen} onOpenChange={setImportOpen}>
        <DialogContent className="sm:max-w-lg bg-white rounded-xl">
          <DialogHeader>
            <DialogTitle className="text-foreground text-base font-bold">Import Test Cases</DialogTitle>
            <DialogDescription className="text-muted-foreground text-sm">
              Import test cases from a CSV, JSON, or XML file.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-5 py-2">
            {/* Drop Zone */}
            <div
              onDragOver={(e) => { e.preventDefault(); setImportDragOver(true); }}
              onDragLeave={() => setImportDragOver(false)}
              onDrop={handleImportDrop}
              className={`flex flex-col items-center justify-center gap-3 rounded-lg border-2 border-dashed p-8 transition-colors cursor-pointer ${importDragOver ? 'border-primary bg-primary/5' : 'border-input bg-muted/20'}`}
              onClick={() => document.getElementById('import-file-input')?.click()}
            >
              {importFile ? (
                <div className="flex items-center gap-2">
                  <File className="h-5 w-5 text-primary" strokeWidth={1.5} />
                  <span className="text-sm font-medium text-foreground">{importFile.name}</span>
                  <button
                    className="text-muted-foreground hover:text-destructive ml-1"
                    onClick={(e) => { e.stopPropagation(); setImportFile(null); }}
                  >
                    <X className="h-3.5 w-3.5" />
                  </button>
                </div>
              ) : (
                <>
                  <Upload className="h-8 w-8 text-muted-foreground/50" strokeWidth={1.5} />
                  <p className="text-sm text-muted-foreground text-center">
                    Drag and drop <span className="font-medium">.csv</span>, <span className="font-medium">.json</span>, or <span className="font-medium">.xml</span> file here
                  </p>
                  <span className="text-xs text-muted-foreground/60">or click to browse</span>
                </>
              )}
              <input
                id="import-file-input"
                type="file"
                accept=".csv,.json,.xml"
                className="hidden"
                onChange={handleImportFileSelect}
              />
            </div>

            {/* Template Download */}
            <button className="text-xs text-primary hover:underline flex items-center gap-1" onClick={downloadCSVTemplate}>
              <Download className="h-3 w-3" /> Download CSV Template
            </button>

            {/* Suite Target */}
            <div>
              <label className="text-[10px] font-semibold text-muted-foreground uppercase mb-1.5 block font-mono tracking-widest">Destination Suite</label>
              <Select value={importSuiteTarget} onValueChange={setImportSuiteTarget}>
                <SelectTrigger className="bg-white">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="root">Root (No Suite)</SelectItem>
                  {suites.map((s) => (
                    <SelectItem key={s.id} value={s.id}>{s.name}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Conflict Logic */}
            <div>
              <label className="text-[10px] font-semibold text-muted-foreground uppercase mb-2 block font-mono tracking-widest">If Case ID Exists</label>
              <RadioGroup value={importConflict} onValueChange={(v) => setImportConflict(v as 'skip' | 'overwrite' | 'create_new')} className="gap-2">
                <div className="flex items-center gap-2">
                  <RadioGroupItem value="skip" id="conflict-skip" />
                  <Label htmlFor="conflict-skip" className="text-sm text-foreground cursor-pointer">Skip</Label>
                </div>
                <div className="flex items-center gap-2">
                  <RadioGroupItem value="overwrite" id="conflict-overwrite" />
                  <Label htmlFor="conflict-overwrite" className="text-sm text-foreground cursor-pointer">Overwrite</Label>
                </div>
                <div className="flex items-center gap-2">
                  <RadioGroupItem value="create_new" id="conflict-new" />
                  <Label htmlFor="conflict-new" className="text-sm text-foreground cursor-pointer">Create New</Label>
                </div>
              </RadioGroup>
            </div>
          </div>
          <DialogFooter>
            <Button variant="ghost" size="sm" onClick={() => setImportOpen(false)}>Cancel</Button>
            <Button
              size="sm"
              className="bg-primary text-primary-foreground hover:bg-primary/90 border-0 gap-1.5"
              onClick={handleImport}
              disabled={!importFile || importing}
            >
              {importing ? <><Loader2 className="h-3.5 w-3.5 animate-spin" /> Importing...</> : <><Upload className="h-3.5 w-3.5" /> Import Data</>}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Bulk Delete Confirmation */}
      <Dialog open={bulkDeleteOpen} onOpenChange={setBulkDeleteOpen}>
        <DialogContent className="sm:max-w-sm bg-card">
          <DialogHeader>
            <DialogTitle className="text-foreground flex items-center gap-3">
              <AlertTriangle className="h-6 w-6 text-destructive shrink-0" strokeWidth={1.5} />
              Delete {selectedCases.size} Test Case{selectedCases.size !== 1 ? 's' : ''}
            </DialogTitle>
            <DialogDescription className="text-sm text-foreground mt-3">
              Are you sure you want to delete <span className="font-bold">{selectedCases.size}</span> selected test case{selectedCases.size !== 1 ? 's' : ''}? This action cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter className="mt-6">
            <Button variant="outline" size="sm" onClick={() => setBulkDeleteOpen(false)}>Cancel</Button>
            <Button size="sm" variant="destructive" onClick={handleBulkDelete}>Delete All</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Create Test Run Modal */}
      <Dialog open={createRunOpen} onOpenChange={setCreateRunOpen}>
        <DialogContent className="sm:max-w-[600px] bg-card border-0 shadow-elevated rounded-xl p-0 gap-0 flex flex-col max-h-[90vh]">
          <DialogHeader className="px-6 pt-6 pb-4 shrink-0">
            <DialogTitle className="text-lg font-bold text-foreground tracking-[-0.02em]">Create Test Run</DialogTitle>
            <DialogDescription className="text-sm text-muted-foreground">
              Create a new test run with {selectedCases.size} selected case{selectedCases.size !== 1 ? 's' : ''}.
            </DialogDescription>
          </DialogHeader>

          <div className="px-6 pb-4 space-y-4 overflow-y-auto flex-1 min-h-0">
            <div>
              <label className="text-[10px] font-semibold text-muted-foreground uppercase mb-1.5 block font-mono tracking-widest">Run Name</label>
              <Input
                value={newRunName}
                onChange={(e) => setNewRunName(e.target.value)}
                placeholder="e.g. Sprint 13 Regression"
                className="mt-0 h-9 bg-white border border-input focus-visible:ring-1 focus-visible:ring-ring"
                autoFocus
              />
            </div>

            <div>
              <label className="text-[10px] font-semibold text-muted-foreground uppercase mb-1.5 block font-mono tracking-widest">Environment</label>
              <Select value={newRunEnv} onValueChange={setNewRunEnv}>
                <SelectTrigger className="mt-0 h-9 bg-white border border-input">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="Staging">Staging</SelectItem>
                  <SelectItem value="Production">Production</SelectItem>
                  <SelectItem value="QA-Env">QA-Env</SelectItem>
                  <SelectItem value="Dev">Dev</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div>
              <label className="text-[10px] font-semibold text-muted-foreground uppercase mb-1.5 block font-mono tracking-widest">Build / Version</label>
              <Input
                value={newRunBuildVersion}
                onChange={(e) => setNewRunBuildVersion(e.target.value)}
                placeholder="e.g. v2.4.0-rc1"
                className="mt-0 h-9 bg-white border border-input focus-visible:ring-1 focus-visible:ring-ring"
              />
            </div>

            <div>
              <label className="text-[10px] font-semibold text-muted-foreground uppercase mb-1.5 block font-mono tracking-widest">Description</label>
              <Textarea
                value={newRunDesc}
                onChange={(e) => setNewRunDesc(e.target.value)}
                placeholder="Describe the purpose of this test run..."
                className="mt-0 bg-white border border-input focus-visible:ring-1 focus-visible:ring-ring min-h-[70px] resize-none"
              />
            </div>

            {/* Selected Cases Preview */}
            <div className="flex-1 min-h-0 flex flex-col">
              <label className="text-[10px] font-semibold text-muted-foreground uppercase mb-1.5 block font-mono tracking-widest">
                Selected Cases <span className="text-muted-foreground/60">({selectedCases.size})</span>
              </label>
              <div className="mt-1.5 rounded-lg bg-secondary overflow-hidden overflow-y-auto flex-1">
                {suites.filter(s => s.cases.some(c => selectedCases.has(c.id))).map((suite) => (
                  <div key={suite.id}>
                    <div className="flex items-center gap-2 px-3 py-2 bg-muted/30">
                      <span className="text-sm font-semibold text-foreground">{suite.name}</span>
                      <span className="text-[10px] text-muted-foreground ml-auto font-mono">
                        {suite.cases.filter(c => selectedCases.has(c.id)).length} cases
                      </span>
                    </div>
                    {suite.cases.filter(c => selectedCases.has(c.id)).map((tc) => (
                      <div key={tc.id} className="flex items-center gap-2 px-3 py-1.5 pl-6">
                        <span className="text-[10px] font-mono text-muted-foreground w-12 shrink-0 truncate" title={tc.id}>{getCaseDisplayId(tc)}</span>
                        <span className="text-xs text-foreground truncate">{tc.title}</span>
                      </div>
                    ))}
                  </div>
                ))}
              </div>
            </div>
          </div>

          <DialogFooter className="px-6 py-4 border-t border-border shrink-0">
            <Button variant="ghost" onClick={() => setCreateRunOpen(false)}>Cancel</Button>
            <Button
              className="bg-gradient-to-br from-primary to-primary/80 text-primary-foreground border-0"
              disabled={!newRunName.trim()}
              onClick={() => {
                if (!projectId || !newRunName.trim()) return;
                createRunMut.mutate(
                  { projectId, body: {
                    name: newRunName, environment: newRunEnv,
                    buildVersion: newRunBuildVersion, description: newRunDesc,
                    untested: selectedCases.size, passed: 0, failed: 0, skipped: 0,
                  } },
                  {
                    onSuccess: () => {
                      toast.success(`Test run "${newRunName}" created with ${selectedCases.size} cases`);
                      setCreateRunOpen(false);
                      clearSelection();
                      navigate(`/projects/${projectId}/runs`);
                    },
                    onError: (e) => toast.error(e.message),
                  }
                );
              }}
            >
              Create Run ({selectedCases.size} cases)
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default Repository;
