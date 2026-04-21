import { useState, useMemo, useRef, useEffect, useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import { useVirtualList } from '@/hooks/useVirtualList';
import Breadcrumbs from '@/components/Breadcrumbs';
import { Search, Paperclip, Lock, CheckCircle2, XCircle, MinusCircle, AlertCircle, Upload, FileText, Image, File, Download, Bug, Trash2, ExternalLink, Eye, Pencil, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from '@/components/ui/dialog';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { toast } from 'sonner';
import CreateDefectModal from '@/components/CreateDefectModal';
import EditDefectModal from '@/components/EditDefectModal';
import ViewDefectModal from '@/components/ViewDefectModal';
import {
  useTestRunDetails,
  useRepository,
  useTestSteps,
  useUpdateTestRun,
  useCompleteTestRun,
  useUnlockTestRun,
  useAttachmentsByCase,
  useTestRunCaseSteps,
  useUpdateTestRunCaseStatus,
  useUpdateTestRunStepStatus,
  useDefectsByRun,
  keys,
} from '@/hooks/useApi';
import {
  defectsApi,
  attachmentsApi,
  testRunCasesApi,
  testRunsApi,
  getAuthToken,
  type Attachment,
  type TestRunCase,
} from '@/lib/api';

const BASE_URL = import.meta.env.VITE_API_URL ??
  (import.meta.env.DEV ? 'http://localhost:8080/api' : '/api');

function downloadWithAuth(url: string, fileName: string) {
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
import { isUuid, formatDate } from '@/lib/utils';
import { AuthenticatedImage, AuthenticatedPdf, isImageType, isPdfType, isPreviewableType } from '@/components/AttachmentPreview';

type StepStatus = 'untested' | 'passed' | 'failed' | 'skipped';

interface EvidenceFile {
  name: string;
  size: number;
  type: string;
  file: File;
}

interface CreatedDefect {
  id: string;
  displayId: string;
  caseId: string;
  caseTitle: string;
  stepIdx?: number;
  title: string;
  description: string;
  severity: 'Critical' | 'Major' | 'Minor';
  status: 'Open' | 'In Progress' | 'Fixed' | 'Closed';
  link: string;
  files: File[];
  createdAt: string;
}

const severityBadge: Record<string, string> = {
  Critical: 'text-destructive',
  Major: 'text-warning',
  Minor: 'text-accent',
};

const statusBadge: Record<string, string> = {
  Open: 'text-destructive',
  'In Progress': 'text-warning',
  Fixed: 'text-success',
  Closed: 'text-muted-foreground',
};

const statusColors: Record<StepStatus, string> = {
  untested: 'bg-muted text-muted-foreground',
  passed: 'bg-success text-success-foreground ring-1 ring-success/30',
  failed: 'bg-destructive text-destructive-foreground ring-1 ring-destructive/30',
  skipped: 'bg-warning text-warning-foreground ring-1 ring-warning/30',
};

const statusOutline: Record<StepStatus, string> = {
  untested: 'text-muted-foreground border border-border hover:bg-secondary',
  passed: 'text-success border border-success/20 hover:bg-success/10',
  failed: 'text-destructive border border-destructive/20 hover:bg-destructive/10',
  skipped: 'text-warning border border-warning/20 hover:bg-warning/10',
};

const caseStatusIcon: Record<string, string> = {
  untested: 'bg-muted-foreground/30',
  passed: 'bg-success',
  failed: 'bg-destructive',
  skipped: 'bg-warning',
};

const normalizeStepStatus = (raw?: string): StepStatus => {
  const value = (raw ?? 'UNTESTED').toLowerCase();
  return (['untested', 'passed', 'failed', 'skipped'] as StepStatus[]).includes(value as StepStatus)
    ? (value as StepStatus)
    : 'untested';
};

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

const labelCls = 'text-[10px] font-semibold text-muted-foreground uppercase mb-1.5 block font-mono tracking-widest';

const DefectAttachmentsList = ({ projectId, defectId }: { projectId: string; defectId: string }) => {
  const [attachments, setAttachments] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    setIsLoading(true);
    attachmentsApi.listByDefect(projectId, defectId)
      .then((atts) => setAttachments(atts || []))
      .catch(() => setAttachments([]))
      .finally(() => setIsLoading(false));
  }, [projectId, defectId]);

  if (isLoading) return <div className="text-sm text-muted-foreground">Loading...</div>;
  if (!attachments || attachments.length === 0) return <p className="text-sm text-muted-foreground mt-1.5">No attachments</p>;

  return (
    <div className="mt-2 space-y-1.5">
      {attachments.map((att) => {
        const IconComp = getFileIcon(att.fileType || att.type || '');
        return (
          <div key={att.id} className="flex items-center gap-2.5 px-3 py-2 bg-secondary rounded-md">
            <IconComp className="h-4 w-4 text-muted-foreground shrink-0" strokeWidth={1.5} />
            <span className="text-sm text-foreground truncate flex-1">{att.fileName || att.name}</span>
            <span className="text-xs text-muted-foreground shrink-0">{formatFileSize(att.fileSize || att.size || 0)}</span>
          </div>
        );
      })}
    </div>
  );
};

const RunExecution = () => {
  const { projectId, runId } = useParams<{ projectId: string; runId: string }>();

  // ── Single batch fetch: run + all run-case records in one HTTP request ────────
  // Replaces the previous two-request waterfall (useTestRun + useTestRunCases) that
  // was responsible for 10+ pending /cases and /steps requests and >10 s latency.
  // The backend GET /runs/{id}/details returns both in one payload so the component
  // renders with complete data immediately on mount.
  const {
    data: runDetails,
    isLoading: runDetailsLoading,
  } = useTestRunDetails(projectId!, runId!);

  const run      = runDetails?.run;
  const runCases = runDetails?.runCases ?? [];

  // Aliased loading flags for backward-compat with the loading guard below.
  const runLoading      = runDetailsLoading;
  const runCasesLoading = runDetailsLoading;

  // Single aggregate call: all suites + cases in one round-trip (no more 1+N waterfall)
  const { data: repositoryData, isLoading: suitesLoading } = useRepository(projectId!);

  const suitesWithCases = useMemo(
    () => (repositoryData?.suites ?? []).map((suite) => ({ ...suite, cases: suite.cases })),
    [repositoryData?.suites],
  );

  const getRunCaseTestCaseId = useCallback((runCase: TestRunCase & { originalTestCaseId?: unknown }) => {
    if (runCase.testCaseId) return runCase.testCaseId;
    return typeof runCase.originalTestCaseId === 'string' ? runCase.originalTestCaseId : '';
  }, []);

  /** testCaseId → TestRunCase.id — used when firing per-step/case DB mutations. */
  const testCaseToRunCaseId = useMemo(() => {
    const map = new Map<string, string>();
    runCases.forEach((rc) => {
      const testCaseId = getRunCaseTestCaseId(rc);
      if (testCaseId) map.set(testCaseId, rc.id);
    });
    return map;
  }, [getRunCaseTestCaseId, runCases]);

  /**
   * Filter the repository suites/cases to only those selected for this run.
   *
   * Priority order for determining which cases belong to this run:
   *   1. `run.caseIds` — the snapshot saved on the run entity at creation time.
   *   2. `runCases`    — the DB-backed TestRunCase records (fallback when caseIds is absent,
   *                      e.g. runs created before the caseIds field was introduced).
   *   3. All repository cases — last resort only when neither source has data yet.
   *
   * Using `runCases` as the primary fallback prevents the "all project cases appear"
   * regression that occurred when a run was created without caseIds being stored.
   */
  const filteredSuitesWithCases = useMemo(() => {
    // Build the authoritative set of case UUIDs for this run.
    let runCaseIdSet: Set<string> | null = null;

    if (run?.caseIds && run.caseIds.length > 0) {
      // Preferred: the snapshot on the run entity.
      runCaseIdSet = new Set(run.caseIds);
    } else if (runCases.length > 0) {
      // Fallback: derive from DB-backed TestRunCase records.
      runCaseIdSet = new Set(
        runCases
          .map((rc) => getRunCaseTestCaseId(rc))
          .filter(Boolean),
      );
    }

    // If we have an authoritative set, filter the repository accordingly.
    if (runCaseIdSet && runCaseIdSet.size > 0) {
      return suitesWithCases
        .map((s) => ({ ...s, cases: s.cases.filter((c) => runCaseIdSet!.has(c.id)) }))
        .filter((s) => s.cases.length > 0);
    }

    // Last resort: show everything (backward compatibility / data not yet loaded).
    return suitesWithCases;
  }, [getRunCaseTestCaseId, suitesWithCases, run?.caseIds, runCases]);

  // Flat list of cases in this run (for indexed access)
  const allCases = useMemo(() =>
    filteredSuitesWithCases.flatMap((s) => s.cases),
    [filteredSuitesWithCases]
  );

  /** TestRunCase.id → original case title — used when hydrating run-scoped defects. */
  const runCaseIdToCaseTitle = useMemo(() => {
    const titleByCaseId = new Map(allCases.map((tc) => [tc.id, tc.title]));
    const map = new Map<string, string>();
    runCases.forEach((rc) => map.set(rc.id, titleByCaseId.get(getRunCaseTestCaseId(rc)) ?? ''));
    return map;
  }, [allCases, getRunCaseTestCaseId, runCases]);

  // Stable ref so the seeding effect can read the latest map without it being a dep
  // (the Map object changes reference on every runCases update, which would cause an
  // infinite re-render loop if included directly in the effect dependency array).
  const runCaseIdToCaseTitleRef = useRef(runCaseIdToCaseTitle);
  runCaseIdToCaseTitleRef.current = runCaseIdToCaseTitle;

  // ── Virtual list for the left case panel ──────────────────────────────────
  type RunItem =
    | { type: 'suite-header'; suiteName: string }
    | { type: 'case'; tc: typeof allCases[0]; localIdx: number; globalIdx: number };

  const runItems = useMemo<RunItem[]>(() => {
    const items: RunItem[] = [];
    let globalOffset = 0;
    for (const suite of filteredSuitesWithCases) {
      items.push({ type: 'suite-header', suiteName: suite.name });
      suite.cases.forEach((tc, localIdx) => {
        items.push({ type: 'case', tc, localIdx, globalIdx: globalOffset + localIdx });
      });
      globalOffset += suite.cases.length;
    }
    return items;
  }, [filteredSuitesWithCases]);

  const caseListRef = useRef<HTMLDivElement>(null);

  const estimateRunItemSize = useCallback((i: number) => {
    return runItems[i]?.type === 'suite-header' ? 36 : 52;
  }, [runItems]);

  const { virtualItems: runVirtualItems, totalSize: runTotalSize } =
    useVirtualList(caseListRef, {
      count: runItems.length,
      estimateSize: estimateRunItemSize,
      overscan: 8,
    });

  /** Returns a human-readable source label for a given case ID. */
  const getCaseSourceLabel = (caseId: string): string => {
    for (const suite of filteredSuitesWithCases) {
      const idx = suite.cases.findIndex((c) => c.id === caseId);
      if (idx !== -1) {
        const c = suite.cases[idx];
        if (c.displayId?.trim()) return c.displayId.trim();
        // Derive prefix from suite name (same logic as Repository.tsx)
        const words = suite.name.match(/[A-Za-z0-9]+/g) ?? [];
        let prefix = 'TC';
        if (words.length > 0) {
          const acronym = words.find((w) => /^[A-Z0-9]{2,4}$/.test(w));
          if (acronym) prefix = acronym.slice(0, 4);
          else if (words.length > 1) prefix = words.slice(0, 3).map((w) => w[0]).join('').toUpperCase();
          else prefix = words[0].slice(0, 3).toUpperCase();
        }
        return `${prefix}-${idx + 1}`;
      }
    }
    return isUuid(caseId) ? caseId.slice(0, 8) : caseId;
  };

  // ── Additional run-scoped query hooks ────────────────────────────────────────

  // ── Server-persisted defects for this run ────────────────────────────────────
  const { data: serverRunDefects = [] } =
    useDefectsByRun(projectId!, runId!);

  // Mutations
  const updateRun               = useUpdateTestRun();
  const updateTestRunCaseStatus = useUpdateTestRunCaseStatus();
  const updateTestRunStepStatus = useUpdateTestRunStepStatus();
  const completeRun = useCompleteTestRun();
  const unlockRun   = useUnlockTestRun();
  const [selectedIdx, setSelectedIdx] = useState(0);
  const [stepStatuses, setStepStatuses] = useState<Record<string, StepStatus[]>>({});
  const [stepEvidence, setStepEvidence] = useState<Record<string, EvidenceFile[]>>({});
  const [serverEvidence, setServerEvidence] = useState<Record<string, Attachment[]>>({});
  // Server-side evidence counts per step, keyed "caseId::stepNumber".
  // Populated via listByRunCaseStep (not listByCase, which excludes EVIDENCE type).
  const [stepEvidenceCounts, setStepEvidenceCounts] = useState<Record<string, number>>({});
  const [evidenceModalOpen, setEvidenceModalOpen] = useState(false);
  const [evidenceTarget, setEvidenceTarget] = useState<{ caseId: string; stepIdx: number } | null>(null);
  const [evidencePreview, setEvidencePreview] = useState<{ name: string; url: string; type: string; isLocal?: boolean } | null>(null);

  const closeEvidencePreview = () => {
    if (evidencePreview?.isLocal) URL.revokeObjectURL(evidencePreview.url);
    setEvidencePreview(null);
  };
  const [defectModalOpen, setDefectModalOpen] = useState(false);
  const [defectContext, setDefectContext] = useState<{
    caseId: string;
    stepIdx?: number;
    source: string;
    /** TestRunCase.id — persisted as testRunCaseId on the Defect record. */
    runCaseId?: string;
  } | null>(null);
  const [createdDefects, setCreatedDefects] = useState<CreatedDefect[]>([]);
  const [viewDefectOpen, setViewDefectOpen] = useState(false);
  const [viewDefect, setViewDefect] = useState<CreatedDefect | null>(null);
  const [viewDefectAttachments, setViewDefectAttachments] = useState<any[]>([]);
  const [isLoadingViewDefectAttachments, setIsLoadingViewDefectAttachments] = useState(false);
  const [editDefectOpen, setEditDefectOpen] = useState(false);
  const [editDefect, setEditDefect] = useState<CreatedDefect | null>(null);
  const [editDefectAttachments, setEditDefectAttachments] = useState<any[]>([]);
  const [isLoadingEditDefectAttachments, setIsLoadingEditDefectAttachments] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const isReadOnly = run?.status === 'completed';
  const qc = useQueryClient();

  // Refs to always have the latest values available in cleanup effects
  const runRef          = useRef(run);
  const stepStatusesRef = useRef(stepStatuses);
  const allCasesRef     = useRef(allCases);
  useEffect(() => { runRef.current         = run; },         [run]);
  useEffect(() => { stepStatusesRef.current = stepStatuses; }, [stepStatuses]);
  useEffect(() => { allCasesRef.current     = allCases; },    [allCases]);

  /**
   * Accumulated flat step-status map that is kept in sync locally on every click.
   *
   * The previous implementation read `r.stepStatuses` from `runRef.current` as the
   * merge base when building the body for `updateRun.mutate()`. Because the server
   * response lags behind rapid clicks, a second click would read a stale copy and
   * silently drop the first click's save (partial-write / progress-reset bug).
   *
   * Using a local ref as the accumulator means each click always merges onto the
   * latest local state, never onto a potentially-stale server snapshot.
   */
  const fullStepStatusesRef = useRef<Record<string, string>>({});

  // Seed the accumulated map when the run first loads (or when stepStatuses are
  // restored from the server after a page refresh).
  // Note: stepStatuses comes from server as JSON string, needs to be parsed.
  useEffect(() => {
    if (run?.stepStatuses && Object.keys(fullStepStatusesRef.current).length === 0) {
      try {
        const parsed = typeof run.stepStatuses === 'string'
          ? JSON.parse(run.stepStatuses)
          : run.stepStatuses;
        fullStepStatusesRef.current = { ...parsed };
      } catch (e) {
        console.warn('Failed to parse stepStatuses:', run.stepStatuses, e);
        fullStepStatusesRef.current = {};
      }
    }
  }, [run?.stepStatuses]);

  // Active case and its live steps
  const activeCase = allCases[selectedIdx];

  // Derived AFTER activeCase so the dependency is always defined.
  const activeCaseRunCaseId = useMemo(
    () => testCaseToRunCaseId.get(activeCase?.id ?? '') ?? '',
    [testCaseToRunCaseId, activeCase?.id],
  );

  /**
   * In-flight deduplication map: caseId → pending Promise<string>.
   *
   * Prevents the "two pending /cases" deadlock: if two concurrent callers
   * (e.g. auto-trigger on step-fail + handleBugClick background resolution)
   * both call ensureRunCaseId for the same case, only ONE POST /cases is ever
   * dispatched.  The second caller receives the same Promise.
   *
   * Stored in a ref so it survives re-renders without recreating the callback.
   */
  const inFlightRunCaseCreates = useRef<Map<string, Promise<string>>>(new Map());

  const ensureRunCaseId = useCallback(async (caseId: string): Promise<string> => {
    // 1. Fast-path: already in the local cache.
    const existingRunCaseId = testCaseToRunCaseId.get(caseId);
    if (existingRunCaseId) return existingRunCaseId;

    // 2. Deduplication: reuse a pending create if one is already in-flight.
    const inflight = inFlightRunCaseCreates.current.get(caseId);
    if (inflight) return inflight;

    // 3. Create with a 12-second timeout so the UI is never blocked indefinitely.
    //    (Cyoda entity creation can be slow under load but rarely exceeds ~10 s.)
    const createPromise = (async (): Promise<string> => {
      const timeoutMs = 12_000;
      let timeoutId: ReturnType<typeof setTimeout> | undefined;

      const timeoutPromise = new Promise<never>((_, reject) => {
        timeoutId = setTimeout(
          () => reject(new Error(`TestRunCase create timed out after ${timeoutMs} ms`)),
          timeoutMs,
        );
      });

      try {
        const created = await Promise.race([
          testRunCasesApi.create(projectId!, runId!, caseId),
          timeoutPromise,
        ]);
        clearTimeout(timeoutId);

        // Patch the legacy runCases list cache.
        qc.setQueryData<{ data: TestRunCase[] }>(
          keys.runCases.all(projectId!, runId!),
          (previous) => ({
            data: [
              ...(previous?.data ?? []).filter((rc) => rc.id !== created.id),
              created,
            ],
          }),
        );

        // Patch the batch-details cache so testCaseToRunCaseId picks up the new
        // record immediately — prevents a second click firing another create.
        qc.setQueryData<import('@/lib/api').TestRunDetail>(
          [...keys.runs.detail(projectId!, runId!), 'details'] as const,
          (prev) => {
            if (!prev) return prev;
            const filtered = prev.runCases.filter((rc) => rc.id !== created.id);
            return { ...prev, runCases: [...filtered, created] };
          },
        );

        return created.id;
      } catch (error) {
        clearTimeout(timeoutId);
        console.error('Failed to ensure TestRunCase exists for case', caseId, error);
        return '';
      } finally {
        // Remove from in-flight map once settled (success or failure).
        inFlightRunCaseCreates.current.delete(caseId);
      }
    })();

    inFlightRunCaseCreates.current.set(caseId, createPromise);
    return createPromise;
  }, [projectId, qc, runId, testCaseToRunCaseId]);

  /** Run-step records for the currently visible case. */
  const { data: activeCaseRunSteps = [] } = useTestRunCaseSteps(
    projectId!, runId!, activeCaseRunCaseId,
  );

  /** testStepId → TestRunStep.id — used when firing per-step DB mutations. */
  const testStepToRunStepId = useMemo(() => {
    const map = new Map<string, string>();
    activeCaseRunSteps.forEach((rs) => map.set(rs.testStepId, rs.id));
    return map;
  }, [activeCaseRunSteps]);

  /** Global step data (action / expectedResult) for the active case. */
  const { data: steps = [], isLoading: stepsLoading } = useTestSteps(
    projectId!,
    activeCase?.suiteId ?? '',
    activeCase?.id ?? '',
  );

  // Sort steps by stepNumber to ensure consistent ordering
  const sortedSteps = useMemo(() =>
    [...steps].sort((a, b) => (a.stepNumber || 0) - (b.stepNumber || 0)),
    [steps]
  );

  // Load persisted attachments for the active case from the server
  const { data: serverAttachments = [] } = useAttachmentsByCase(
    projectId!,
    activeCase?.id ?? '',
  );

  // ── Pure helpers (no state dependency) ───────────────────────────────────────

  /** Compute a single case's status from its array of step statuses. */
  const computeCaseStatus = (statuses: StepStatus[]): string => {
    if (!statuses || statuses.length === 0) return 'untested';
    if (statuses.some((s) => s === 'failed')) return 'failed';
    if (statuses.every((s) => s === 'passed')) return 'passed';
    if (statuses.some((s) => s === 'skipped') && !statuses.some((s) => s === 'untested')) return 'skipped';
    return 'untested';
  };

  /**
   * Restore case-level status immediately on mount, before a user clicks into each
   * individual case. Prefer the persisted flat run.stepStatuses map; fall back to
   * DB-backed TestRunCase.status when no per-step snapshot exists.
   * Note: stepStatuses comes from server as JSON string, needs to be parsed.
   */
  const persistedCaseStatusById = useMemo(() => {
    let statusMap: Record<string, string> = {};
    if (run?.stepStatuses) {
      try {
        statusMap = typeof run.stepStatuses === 'string'
          ? JSON.parse(run.stepStatuses)
          : run.stepStatuses;
      } catch (e) {
        console.warn('Failed to parse stepStatuses:', run.stepStatuses, e);
        statusMap = {};
      }
    }
    const result: Record<string, StepStatus> = {};

    allCases.forEach((tc) => {
      const persistedStepStatuses = Object.entries(statusMap)
        .filter(([key]) => key.startsWith(`${tc.id}::`))
        .map(([, value]) => normalizeStepStatus(value));

      if (persistedStepStatuses.length > 0) {
        result[tc.id] = computeCaseStatus(persistedStepStatuses) as StepStatus;
        return;
      }

      const runCase = runCases.find((rc) => getRunCaseTestCaseId(rc) === tc.id);
      if (runCase?.status) {
        result[tc.id] = normalizeStepStatus(runCase.status);
      }
    });

    return result;
  }, [allCases, getRunCaseTestCaseId, run?.stepStatuses, runCases]);

  /**
   * Flat per-step status map derived synchronously from run.stepStatuses.
   * Used as a fallback when stepStatuses state hasn't been initialised yet
   * (e.g. Effect 3 hasn't fired or step IDs drifted between sessions).
   * Keys: "caseId::stepNumber" → StepStatus.
   */
  const persistedStepStatusByKey = useMemo(() => {
    if (!run?.stepStatuses) return {} as Record<string, StepStatus>;
    try {
      const map: Record<string, string> = typeof run.stepStatuses === 'string'
        ? JSON.parse(run.stepStatuses)
        : run.stepStatuses as unknown as Record<string, string>;
      return Object.fromEntries(
        Object.entries(map).map(([k, v]) => [k, normalizeStepStatus(v)]),
      ) as Record<string, StepStatus>;
    } catch {
      return {} as Record<string, StepStatus>;
    }
  }, [run?.stepStatuses]);

  /**
   * Compute run-level aggregate counts from a step-status map.
   * Uses the provided cases list so it works inside cleanup refs too.
   */
  const computeAggregates = (
    statusMap: Record<string, StepStatus[]>,
    cases: typeof allCases,
  ) => {
    let passed = 0, failed = 0, skipped = 0, untested = 0;
    cases.forEach((tc) => {
      const s = computeCaseStatus(statusMap[tc.id] || []);
      if (s === 'passed')       passed++;
      else if (s === 'failed')  failed++;
      else if (s === 'skipped') skipped++;
      else                      untested++;
    });
    return { passed, failed, skipped, untested };
  };

  // ── Sync server-fetched run defects into local state ─────────────────────────
  // `createdDefects` was purely in-memory, so it reset on every page reload.
  // `serverRunDefects` now fetches from the backend (filtered by testRunId).
  // This effect seeds createdDefects from the server response so the defect table
  // survives a page refresh without any changes to the existing modal/action logic.
  useEffect(() => {
    if (serverRunDefects.length === 0) return; // nothing to seed yet
    setCreatedDefects((prev) => {
      const previousById = new Map(prev.map((item) => [item.id, item]));
      const hydrated = serverRunDefects.map((d) => {
        const previous = previousById.get(d.id);
        return {
          id:          d.id,
          displayId:   d.displayId ?? previous?.displayId ?? d.id.slice(0, 8).toUpperCase(),
          caseId:      (() => {
            // Map testRunCaseId → original testCaseId so getCaseSourceLabel works.
            const rc = d.testRunCaseId ? runCases.find((r) => r.id === d.testRunCaseId) : undefined;
            return rc ? getRunCaseTestCaseId(rc) : (previous?.caseId ?? '');
          })(),
          caseTitle:   previous?.caseTitle ?? (d.testRunCaseId ? runCaseIdToCaseTitleRef.current.get(d.testRunCaseId) ?? '' : ''),
          stepIdx:     (() => {
            if (previous?.stepIdx !== undefined) return previous.stepIdx;
            const m = d.source?.match(/·\s*Step\s*(\d+)/i);
            return m ? parseInt(m[1], 10) - 1 : undefined;
          })(),
          title:       d.title,
          description: d.description,
          severity:    (d.severity as CreatedDefect['severity']) ?? previous?.severity ?? 'Minor',
          status:      (d.status  as CreatedDefect['status'])   ?? previous?.status ?? 'Open',
          link:        d.link ?? previous?.link ?? '',
          files:       previous?.files ?? [],
          createdAt:   (d.createdAt ?? '').split('T')[0] || previous?.createdAt || new Date().toISOString().split('T')[0],
        };
      });

      // Preserve freshly-created local defects that the immediate refetch may not
      // return yet (eventual-consistency / indexing lag on the backend).
      const hydratedIds = new Set(hydrated.map((item) => item.id));
      const optimisticOnly = prev.filter((item) => !hydratedIds.has(item.id));

      return [...optimisticOnly, ...hydrated];
    });
  // Re-run only when server data changes. runCaseIdToCaseTitleRef is accessed via
  // ref to avoid including the Map in deps (new Map reference on every runCases update
  // caused an infinite setState→render→Map→setState loop).
  }, [serverRunDefects]);

  // ── On unmount: save progress to the run + invalidate list cache ─────────────
  useEffect(() => {
    return () => {
      const r       = runRef.current;
      const cases   = allCasesRef.current;
      const statMap = stepStatusesRef.current;

      // Always invalidate so the list re-fetches when the user navigates back
      qc.invalidateQueries({ queryKey: keys.runs.all(projectId!) });

      if (!r || !runId || !projectId || r.status === 'completed') return;

      // Derive aggregate counts from local step-status state
      const agg = computeAggregates(statMap, cases);
      const hasProgress = agg.passed + agg.failed + agg.skipped > 0;

      // Fire-and-forget: direct API call is safer than a mutation hook in cleanup.
      // Use fullStepStatusesRef (the locally accumulated map) rather than
      // r.stepStatuses (the last server response) so that clicks made since the
      // most recent server round-trip are not lost.
      // Guard: skip the call if runId is not a valid UUID to avoid spurious 404s.
      if (!runId || !isUuid(runId)) return;
      testRunsApi.update(projectId, runId, {
        name:         r.name,
        environment:  r.environment,
        buildVersion: r.buildVersion ?? '',
        description:  r.description ?? '',
        // Promote to 'active' as soon as any case is worked on
        status: hasProgress && r.status === 'initial' ? 'active' : r.status,
        // Guard: same as in setStepStatus — never send empty caseIds.
        // The backend will preserve the existing list if undefined is sent.
        caseIds: (r.caseIds && r.caseIds.length > 0) ? r.caseIds : undefined,
        // Convert stepStatuses from object to JSON string (backend expects string)
        stepStatuses: JSON.stringify(fullStepStatusesRef.current),
        ...(r.createdAt ? { createdAt: r.createdAt } : {}),
        ...agg,
      }).then(() => {
        qc.invalidateQueries({ queryKey: keys.runs.detail(projectId, runId) });
      }).catch((error) => {
        // Log the error so we can debug why progress isn't being saved
        console.error('[RunExecution cleanup] Failed to save test run progress on unmount:', {
          runId,
          projectId,
          error: error instanceof Error ? error.message : String(error),
          stepStatuses: fullStepStatusesRef.current,
          runStatus: r?.status,
        });
      });
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // ── Initialise step-status state when a new case is loaded ───────────────────
  useEffect(() => {
    if (!activeCase || sortedSteps.length === 0) return;

    setStepStatuses((prev) => {
      // Don't overwrite state the user has already changed in this session.
      if (prev[activeCase.id] !== undefined) return prev;

      // Read persisted statuses from run.stepStatuses.
      // Key format: "caseId::stepId" → uppercase status string.
      // Note: stepStatuses comes from server as JSON string, needs to be parsed.
      let runStepStatuses: Record<string, string> = {};
      if (runRef.current?.stepStatuses) {
        try {
          runStepStatuses = typeof runRef.current.stepStatuses === 'string'
            ? JSON.parse(runRef.current.stepStatuses)
            : runRef.current.stepStatuses;
        } catch (e) {
          console.warn('Failed to parse stepStatuses:', runRef.current.stepStatuses, e);
          runStepStatuses = {};
        }
      }
      const initial: StepStatus[] = sortedSteps.map((s) => {
        const key = `${activeCase.id}::${s.stepNumber}`;
        return normalizeStepStatus(runStepStatuses[key]);
      });
      return { ...prev, [activeCase.id]: initial };
    });
  // Re-run whenever the active case changes or steps arrive.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeCase?.id, sortedSteps.length]);

  // Pre-load evidence counts for every step of the active case.
  // Must use listByRunCaseStep (not listByCase) because the backend's /by-case
  // endpoint explicitly filters out EVIDENCE-type attachments.
  useEffect(() => {
    if (!activeCase || sortedSteps.length === 0 || !projectId || !runId) return;
    const caseId = activeCase.id;
    void Promise.all(
      sortedSteps.map(async (step) => {
        try {
          const atts = await attachmentsApi.listByRunCaseStep(
            projectId, runId, caseId, String(step.stepNumber),
          );
          return [`${caseId}::${step.stepNumber}`, (atts ?? []).length] as const;
        } catch {
          return [`${caseId}::${step.stepNumber}`, 0] as const;
        }
      }),
    ).then((results) => {
      setStepEvidenceCounts((prev) => {
        const next = { ...prev };
        results.forEach(([key, count]) => { next[key] = count; });
        return next;
      });
    });
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeCase?.id, sortedSteps.length, projectId, runId]);

  const getStepStatuses = (caseId: string): StepStatus[] => {
    return stepStatuses[caseId] || [];
  };

  const setStepStatus = (caseId: string, stepNumber: number, globalStepId: string, status: StepStatus) => {
    if (isReadOnly) return;

    // ── 1. Update local state immediately for instant UI feedback ────────────
    const current = stepStatuses[caseId] || Array(sortedSteps.length).fill('untested');
    const updated = [...current];
    updated[stepNumber - 1] = status; // stepNumber is 1-based, array is 0-based
    setStepStatuses((prev) => ({ ...prev, [caseId]: updated }));

    // ── 2. Accumulate into local ref — prevents stale-server-merge race ──────
    // Previously we spread `r.stepStatuses` (server state) as the merge base.
    // Between rapid clicks the server may not have responded yet, so the second
    // click's body would overwrite the first click's key. fullStepStatusesRef is
    // always up to date regardless of in-flight requests.
    const runKey = `${caseId}::${stepNumber}`;
    fullStepStatusesRef.current = { ...fullStepStatusesRef.current, [runKey]: status.toUpperCase() };

    // ── 3. Persist aggregates + step map to the run entity ───────────────────
    //
    // Guard: only fire if runId is a real UUID (not undefined / a displayId).
    // A missing or malformed runId would produce a 404 from the backend's
    // testRunExists(id) check, which is the source of the console error.
    const r = runRef.current;
    if (r && runId && isUuid(runId)) {
      const newAllStatuses = { ...stepStatusesRef.current, [caseId]: updated };
      const agg = computeAggregates(newAllStatuses, allCasesRef.current);

      updateRun.mutate({
        projectId: projectId!,
        id:        runId,
        body: {
          name:         r.name,
          environment:  r.environment,
          buildVersion: r.buildVersion ?? '',
          description:  r.description ?? '',
          status:       r.status === 'initial' ? 'active' : r.status,
          // Guard: never send an empty / undefined caseIds — the backend performs a
          // full-entity replacement and an empty list would silently erase the run's
          // case snapshot, causing all cases to vanish on the next page refresh.
          // The backend also has a matching server-side guard in updateTestRun().
          caseIds: (r.caseIds && r.caseIds.length > 0) ? r.caseIds : undefined,
          // Use the local accumulated map — never the potentially-stale server copy.
          // Convert to JSON string (backend expects string)
          stepStatuses: JSON.stringify(fullStepStatusesRef.current),
          ...(r.createdAt ? { createdAt: r.createdAt } : {}),
          ...agg,
        },
      });
    }

    // ── 4. Also update DB-backed TestRunStep and TestRunCase records ──────────
    // These are the dedicated per-run execution records (not the flat map on the
    // Run entity). Keeping both in sync means the /cases and /steps endpoints
    // always reflect the real execution state for reporting & external consumers.
    const runCaseId = testCaseToRunCaseId.get(caseId);
    const runStepId = testStepToRunStepId.get(globalStepId);
    const derivedCaseStatus = computeCaseStatus(updated).toUpperCase();
    if (runCaseId && runStepId) {
      updateTestRunStepStatus.mutate({
        projectId: projectId!,
        runId:     runId!,
        runCaseId,
        id:        runStepId,
        status:    status.toUpperCase(),
      });
    }
    if (runCaseId) {
      updateTestRunCaseStatus.mutate({
        projectId: projectId!,
        runId:     runId!,
        id:        runCaseId,
        status:    derivedCaseStatus,
      });
    }

    // ── 5. Auto-trigger defect modal on 'failed' — opens synchronously ────────
    // Previously this awaited ensureRunCaseId() before calling setDefectModalOpen,
    // adding a full network round-trip delay before the dialog appeared.
    // Now the modal opens immediately using whatever run-case ID is in the cache;
    // the async resolution patches the context before the user submits.
    if (status === 'failed') {
      const knownRunCaseId = testCaseToRunCaseId.get(caseId);
      setDefectContext({
        caseId,
        stepIdx:   stepNumber - 1,
        source:    `${getCaseSourceLabel(caseId)} · Step ${stepNumber}`,
        runCaseId: knownRunCaseId,
      });
      setDefectModalOpen(true);

      if (!knownRunCaseId) {
        void ensureRunCaseId(caseId).then((resolvedId) => {
          if (resolvedId) {
            setDefectContext((prev) =>
              prev ? { ...prev, runCaseId: resolvedId } : prev,
            );
          }
        });
      }
    }
  };

  const getCaseStatus = (caseId: string): string => {
    // Derive from local step-status state (real-time; restored from run.stepStatuses on load).
    const statuses = stepStatuses[caseId];
    if (statuses && statuses.length > 0) {
      if (statuses.some((s) => s === 'failed')) return 'failed';
      if (statuses.every((s) => s === 'passed')) return 'passed';
      if (statuses.some((s) => s === 'skipped') && !statuses.some((s) => s === 'untested')) return 'skipped';
    }
    // Fall back to persisted run data so progress and sidebar icons are restored
    // immediately on reopen, even before each case is individually selected.
    if (persistedCaseStatusById[caseId]) return persistedCaseStatusById[caseId];
    return 'untested';
  };

  const getEvidenceKey = (caseId: string, stepIdx: number) => `${caseId}::${stepIdx}`;

  const getEvidenceFiles = (caseId: string, stepIdx: number): EvidenceFile[] => {
    return stepEvidence[getEvidenceKey(caseId, stepIdx)] || [];
  };

  const handleEvidenceClick = (caseId: string, stepIdx: number) => {
    if (isReadOnly) return;
    const stepKey = String(stepIdx + 1);
    setEvidenceTarget({ caseId, stepIdx });
    setEvidenceModalOpen(true);
    attachmentsApi.listByRunCaseStep(projectId!, runId!, caseId, stepKey)
      .then(atts => {
        const key = `${caseId}::${stepIdx}`;
        setServerEvidence(prev => ({ ...prev, [key]: atts || [] }));
      })
      .catch(() => {});
  };

  /**
   * Opens the "Report Issue" modal **synchronously** on click — no network await
   * before showing the dialog (Task 3: Instant Modal Opening).
   *
   * If a TestRunCase record already exists for this case we use it immediately.
   * If not, we open the modal anyway and resolve the runCaseId asynchronously,
   * then patch the context so the submit handler gets the correct ID.
   */
  const handleBugClick = (caseId: string, stepIdx?: number) => {
    if (isReadOnly) return;

    // Use whatever ID we already have in the cache — open instantly.
    const knownRunCaseId = testCaseToRunCaseId.get(caseId);
    setDefectContext({
      caseId,
      stepIdx,
      source:    stepIdx !== undefined
        ? `${getCaseSourceLabel(caseId)} · Step ${stepIdx + 1}`
        : getCaseSourceLabel(caseId),
      runCaseId: knownRunCaseId,
    });
    setDefectModalOpen(true);

    // If the run-case record doesn't exist yet, create it in the background and
    // patch the context so the submit handler has the real ID when the user submits.
    if (!knownRunCaseId) {
      void ensureRunCaseId(caseId).then((resolvedId) => {
        if (resolvedId) {
          setDefectContext((prev) =>
            prev ? { ...prev, runCaseId: resolvedId } : prev,
          );
        }
      });
    }
  };

  const handleFileUpload = async (files: FileList | null) => {
    if (!files || !evidenceTarget) return;
    const key = getEvidenceKey(evidenceTarget.caseId, evidenceTarget.stepIdx);
    const fileArray = Array.from(files);
    const newFiles: EvidenceFile[] = fileArray.map((f) => ({
      name: f.name,
      size: f.size,
      type: f.type,
      file: f,
    }));
    setStepEvidence((prev) => ({
      ...prev,
      [key]: [...(prev[key] || []), ...newFiles],
    }));
    try {
      await Promise.all(
        fileArray.map((f) =>
          attachmentsApi.upload(
            projectId!,
            f,
            evidenceTarget.caseId,
            undefined,
            'EVIDENCE',
            runId!,
            String(evidenceTarget.stepIdx + 1),
          )
        )
      );
      // Optimistically update the badge count so it shows immediately
      const evidKey = `${evidenceTarget.caseId}::${evidenceTarget.stepIdx + 1}`;
      setStepEvidenceCounts((prev) => ({ ...prev, [evidKey]: (prev[evidKey] ?? 0) + fileArray.length }));
      // Refresh server-side attachment list so they survive a page reload
      qc.invalidateQueries({ queryKey: ['attachments', projectId!, 'case', evidenceTarget.caseId] });
      qc.invalidateQueries({
        queryKey: ['attachments', projectId!, 'run', runId!, 'case', evidenceTarget.caseId, 'step', String(evidenceTarget.stepIdx + 1)],
      });
      toast.success(`${fileArray.length} file(s) uploaded`);
    } catch {
      toast.error('Upload failed — files saved locally but not persisted');
    }
  };

  const handleCreateDefect = async (defect: any) => {
    if (!defectContext) return;

    // ── Resolve runCaseId without blocking defect creation ────────────────────
    // Previously this called `await ensureRunCaseId(defect.caseId)` and would
    // hard-block if the POST /cases request was pending (e.g. Cyoda slow under
    // load).  Now we use whatever ID is already in the context or cache; if
    // neither is available we proceed anyway — testRunCaseId is optional context
    // for defect scoping and must never prevent the actual defect from being saved.
    const runCaseId =
      defectContext.runCaseId ??
      testCaseToRunCaseId.get(defectContext.caseId) ??
      undefined;   // will be omitted from the payload when undefined

    try {
      const created = await defectsApi.create(projectId!, {
        title:       defect.title,
        description: defect.description,
        severity:    defect.severity,
        status:      defect.status,
        source:      defect.source,
        link:        defect.link,
        // ── Persist run / case links so the table survives a page reload ────
        testRunId:     runId,
        // runCaseId may be undefined for cases not yet opened — that is safe;
        // the defect will still be linked to the run and appear in the run's
        // defect table.  The testRunCaseId will be enriched on next page load
        // via the server-side defect list which scopes by testRunId.
        ...(runCaseId ? { testRunCaseId: runCaseId } : {}),
      });

      setCreatedDefects((prev) => [
        {
          id:          created.id,
          displayId:   created.displayId,
          caseId:      defect.caseId,
          caseTitle:   allCases.find((item) => item.id === defect.caseId)?.title ?? activeCase.title,
          stepIdx:     defectContext.stepIdx,
          title:       created.title,
          description: created.description ?? '',
          severity:    (created.severity as CreatedDefect['severity']) ?? defect.severity,
          status:      (created.status as CreatedDefect['status']) ?? defect.status,
          link:        created.link ?? defect.link ?? '',
          files:       defect.files ?? [],
          createdAt:   (created.createdAt ?? '').split('T')[0] || new Date().toISOString().split('T')[0],
        },
        ...prev.filter((item) => item.id !== created.id),
      ]);

      // Upload any attached files, linked to the defect
      if (defect.files && (defect.files as File[]).length > 0) {
        await Promise.all(
          (defect.files as File[]).map((f) =>
            attachmentsApi.upload(projectId!, f, undefined, created.id, 'DEFECT')
          )
        );
      }

      // Invalidate the run-scoped defect query so the table re-fetches from
      // the server — this replaces the old in-memory createdDefects push that
      // was lost on every page reload.
      qc.invalidateQueries({ queryKey: keys.runDefects.all(projectId!, runId!) });
      setDefectModalOpen(false);
      toast.success('Defect created');
    } catch {
      toast.error('Failed to create defect — please try again');
      throw new Error('Failed to create defect');
    }
  };

  const openViewDefect = (defect: CreatedDefect) => {
    setViewDefect(defect);
    setIsLoadingViewDefectAttachments(true);
    attachmentsApi.listByDefect(projectId!, defect.id)
      .then(atts => setViewDefectAttachments((atts || []).map(a => ({ id: a.id, name: a.fileName, size: a.fileSize, type: a.fileType ?? '' }))))
      .catch(() => setViewDefectAttachments([]))
      .finally(() => setIsLoadingViewDefectAttachments(false));
    setViewDefectOpen(true);
  };

  const openEditDefect = (defect: CreatedDefect) => {
    setEditDefect(defect);
    setIsLoadingEditDefectAttachments(true);
    attachmentsApi.listByDefect(projectId!, defect.id)
      .then(atts => setEditDefectAttachments((atts || []).map(a => ({ id: a.id, name: a.fileName, size: a.fileSize, type: a.fileType ?? '' }))))
      .catch(() => setEditDefectAttachments([]))
      .finally(() => setIsLoadingEditDefectAttachments(false));
    setEditDefectOpen(true);
  };

  const handleEditDefectSave = async (updatedDefect: CreatedDefect, newFiles: File[], removedAttachmentIds: string[]) => {
    if (!projectId) return;

    try {
      // Update defect via API
      await defectsApi.update(projectId!, updatedDefect.id, {
        title: updatedDefect.title,
        description: updatedDefect.description,
        severity: updatedDefect.severity,
        status: updatedDefect.status,
        source: updatedDefect.source,
        link: updatedDefect.link,
      });

      // Upload new files
      for (const file of newFiles) {
        try {
          await attachmentsApi.upload(projectId!, file, undefined, updatedDefect.id, 'DEFECT');
        } catch (error) {
          toast.warning(`Failed to upload ${file.name}`);
        }
      }

      // Delete removed attachments
      for (const attachmentId of removedAttachmentIds) {
        try {
          await attachmentsApi.delete(projectId!, attachmentId);
        } catch (error) {
          toast.warning(`Failed to delete attachment`);
        }
      }

      // Update createdDefects list
      setCreatedDefects((prev) =>
        prev.map((d) =>
          d.id === updatedDefect.id ? { ...d, ...updatedDefect } : d
        )
      );

      // Refetch defects from server
      qc.invalidateQueries({ queryKey: keys.runDefects.all(projectId!, runId!) });

      toast.success('Defect updated');
      setEditDefectOpen(false);
      setEditDefect(null);
      setEditDefectAttachments([]);
    } catch (error: any) {
      toast.error(error?.message || 'Failed to update defect');
    }
  };

  /**
   * Only show defects that were raised against the currently active test case.
   * `createdDefects[n].caseId` stores the original TestCase.id (not testRunCaseId)
   * so we compare directly with activeCase.id.
   */
  const activeCaseDefects = createdDefects.filter(
    (d) => d.caseId === activeCase?.id,
  );

  const progressStats = useMemo(() => {
    let passed = 0, failed = 0, skipped = 0, untested = 0;

    // Derive from local step-status state (populated from run.stepStatuses on load
    // and updated optimistically on every click).
    allCases.forEach((tc) => {
      const s = getCaseStatus(tc.id);
      if (s === 'passed')        passed++;
      else if (s === 'failed')   failed++;
      else if (s === 'skipped')  skipped++;
      else                       untested++;
    });

    const total = allCases.length;
    const completed = passed + failed + skipped;
    return { passed, failed, skipped, untested, total, completed };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [stepStatuses, allCases, persistedCaseStatusById]);

  // Loading / empty guard
  // Wait for run, repository (suites+cases), AND run-scoped case records before
  // rendering. Previously only runLoading+suitesLoading were checked; the missing
  // runCasesLoading guard meant the component could render with stale/empty data
  // before the DB-backed TestRunCase records had arrived, causing the progress bar
  // to show 0/N and mutations to fire with missing IDs.
  if (runLoading || suitesLoading || runCasesLoading) {
    return (
      <div className="flex items-center justify-center h-full gap-2 text-muted-foreground">
        <Loader2 className="h-5 w-5 animate-spin" />
        <span className="text-sm">Loading run…</span>
      </div>
    );
  }

  if (!run) return <div className="p-8 text-muted-foreground">Run not found</div>;

  if (!activeCase) return <div className="p-8 text-muted-foreground">No test cases found for this run.</div>;

  const { passed, failed, skipped, untested, total, completed } = progressStats;

  return (
    <div className="h-full flex flex-col">
      {/* Execution Header */}
      <div className="px-6 py-4 bg-card border-b border-border">
        <div className="mb-2">
          <Breadcrumbs segments={[
            { label: 'Projects', href: '/projects' },
            { label: run.projectId ? 'Project' : 'Project', href: `/projects/${projectId}/repository` },
            { label: 'Test Runs', href: `/projects/${projectId}/runs` },
            { label: run.name || 'Run' },
          ]} />
        </div>

        <div className="flex items-center gap-4 mt-3">
          <span className="text-[10px] px-2 py-0.5 rounded-md bg-secondary text-muted-foreground font-mono uppercase tracking-widest">{run.environment}</span>

          <div className="flex-1 max-w-md">
            <div className="flex h-3 rounded-full overflow-hidden w-full bg-secondary">
              {passed > 0 && <div className="bg-success transition-all" style={{ width: `${(passed / total) * 100}%` }} />}
              {failed > 0 && <div className="bg-destructive transition-all" style={{ width: `${(failed / total) * 100}%` }} />}
              {skipped > 0 && <div className="bg-warning transition-all" style={{ width: `${(skipped / total) * 100}%` }} />}
              {untested > 0 && <div className="bg-muted-foreground/20 transition-all" style={{ width: `${(untested / total) * 100}%` }} />}
            </div>
          </div>

          <span className="text-sm font-semibold text-foreground whitespace-nowrap">
            {completed} of {total} cases completed
          </span>

          <div className="flex items-center gap-3 ml-2">
            <div className="flex items-center gap-1">
              <CheckCircle2 className="h-3.5 w-3.5 text-success" strokeWidth={2} />
              <span className="text-xs font-mono font-semibold text-success">{passed}</span>
            </div>
            <div className="flex items-center gap-1">
              <XCircle className="h-3.5 w-3.5 text-destructive" strokeWidth={2} />
              <span className="text-xs font-mono font-semibold text-destructive">{failed}</span>
            </div>
            <div className="flex items-center gap-1">
              <AlertCircle className="h-3.5 w-3.5 text-warning" strokeWidth={2} />
              <span className="text-xs font-mono font-semibold text-warning">{skipped}</span>
            </div>
            <div className="flex items-center gap-1">
              <MinusCircle className="h-3.5 w-3.5 text-muted-foreground" strokeWidth={2} />
              <span className="text-xs font-mono font-semibold text-muted-foreground">{untested}</span>
            </div>
          </div>

          {isReadOnly ? (
            <Button
              variant="ghost"
              size="sm"
              className="gap-1.5 ml-auto"
              disabled={unlockRun.isPending}
              onClick={() =>
                unlockRun.mutate(
                  { projectId: projectId!, id: runId! },
                  {
                    onSuccess: () => {
                      // Invalidate both the run details AND the runs list so UI updates everywhere
                      qc.invalidateQueries({ queryKey: keys.runs.detail(projectId!, runId!) });
                      qc.invalidateQueries({ queryKey: keys.runs.all(projectId!) });
                      toast.success('Run unlocked');
                    },
                    onError:   (e) => toast.error(e.message),
                  }
                )
              }
            >
              <Lock className="h-3.5 w-3.5" strokeWidth={1.5} />
              {unlockRun.isPending ? 'Unlocking…' : 'Unlock Run'}
            </Button>
          ) : (
            <Button
              size="sm"
              className="gap-1.5 ml-auto bg-gradient-to-br from-primary to-primary/80 text-primary-foreground border-0"
              disabled={completeRun.isPending}
              onClick={() =>
                completeRun.mutate(
                  { projectId: projectId!, id: runId! },
                  {
                    onSuccess: () => {
                      // Invalidate both the run details AND the runs list so UI updates everywhere
                      qc.invalidateQueries({ queryKey: keys.runs.detail(projectId!, runId!) });
                      qc.invalidateQueries({ queryKey: keys.runs.all(projectId!) });
                      toast.success('Run completed');
                    },
                    onError:   (e) => toast.error(e.message),
                  }
                )
              }
            >
              {completeRun.isPending ? <><Loader2 className="h-3.5 w-3.5 animate-spin" /> Completing…</> : 'Complete Run'}
            </Button>
          )}
        </div>
      </div>

      <div className="flex flex-1 overflow-hidden">
        {/* Left: Case List Grouped by Suite — virtualised */}
        <div className="w-64 surface-low shrink-0 flex flex-col overflow-hidden">
          <div className="p-3 shrink-0">
            <div className="relative">
              <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground" strokeWidth={1.5} />
              <Input placeholder="Find case..." className="pl-8 h-8 text-xs bg-card border-0 focus-visible:ring-1 focus-visible:ring-accent/40" />
            </div>
          </div>
          {/* Scroll container — only this div scrolls, height is flex-1 */}
          <div ref={caseListRef} className="flex-1 overflow-auto px-1.5 pb-2">
            <div style={{ height: runTotalSize, position: 'relative' }}>
              {runVirtualItems.map((vi) => {
                const item = runItems[vi.index];
                return (
                  <div
                    key={vi.key}
                    style={{ position: 'absolute', top: vi.start, left: 0, width: '100%', height: vi.size }}
                  >
                    {item.type === 'suite-header' ? (
                      <div className="px-3 py-2 text-[10px] font-semibold text-muted-foreground uppercase tracking-widest bg-muted/40 rounded-md mb-0.5">
                        {item.suiteName}
                      </div>
                    ) : (
                      <button
                        onClick={() => setSelectedIdx(item.globalIdx)}
                        className={`w-full flex items-center gap-2.5 px-3 py-2.5 rounded-md text-sm transition-colors ${
                          item.globalIdx === selectedIdx ? 'bg-card shadow-soft' : 'hover:bg-card/60'
                        }`}
                      >
                        <div className={`h-2.5 w-2.5 rounded-full shrink-0 ${caseStatusIcon[getCaseStatus(item.tc.id)]}`} />
                        <div className="flex-1 text-left min-w-0">
                          <span className="text-[10px] text-muted-foreground font-mono tracking-wider" title={item.tc.id}>
                            {getCaseSourceLabel(item.tc.id)}
                          </span>
                          <p className="text-xs font-medium text-foreground truncate">{item.tc.title}</p>
                        </div>
                      </button>
                    )}
                  </div>
                );
              })}
            </div>
          </div>
        </div>

        {/* Center: Step Execution */}
        <div className="flex-1 overflow-auto p-6 bg-card">
          <div className="mb-5 flex items-start justify-between">
            <div>
              <span className="text-[10px] font-mono text-muted-foreground tracking-wider">{getCaseSourceLabel(activeCase.id)}</span>
              <div className="flex items-center gap-3 mt-1">
                <h2 className="text-lg font-bold text-foreground tracking-[-0.02em]">{activeCase.title}</h2>
                <span className={`inline-block px-2.5 py-0.5 text-[10px] uppercase tracking-widest font-mono ${
                  activeCase.priority === 'HIGH' ? 'text-destructive' :
                  activeCase.priority === 'MEDIUM' ? 'text-warning' :
                  'text-muted-foreground'
                }`}>
                  {activeCase.priority}
                </span>
              </div>
            </div>
            {!isReadOnly && (
              <Button
                variant="outline"
                size="sm"
                className="gap-1.5 text-destructive border-destructive/20 hover:bg-destructive/10 hover:text-destructive shrink-0"
                onClick={() => handleBugClick(activeCase.id)}
              >
                <Bug className="h-3.5 w-3.5" strokeWidth={1.5} />
                Report Case Issue
              </Button>
            )}
          </div>

          {/* Case Description and Pre-conditions */}
          <div className="grid grid-cols-2 gap-4 mb-6">
            {activeCase.description && (
              <div className="bg-muted/40 rounded-lg p-4">
                <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-widest mb-2">Description</h3>
                <p className="text-sm text-foreground">{activeCase.description}</p>
              </div>
            )}
            {activeCase.preconditions && (
              <div className="bg-muted/40 rounded-lg p-4">
                <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-widest mb-2">Pre-conditions</h3>
                <p className="text-sm text-foreground">{activeCase.preconditions}</p>
              </div>
            )}
          </div>

          {/* Attachments Section — CASE type only; Evidence stays in Evidence modal */}
          {serverAttachments.filter(
            (att: Attachment) => !att.attachmentType || att.attachmentType === 'CASE'
          ).length > 0 && (
            <div className="bg-muted/40 rounded-lg p-4 mb-6">
              <h3 className="text-xs font-semibold text-muted-foreground uppercase tracking-widest mb-3">Attachments</h3>
              <div className="space-y-2">
                {serverAttachments
                  .filter((att: Attachment) => !att.attachmentType || att.attachmentType === 'CASE')
                  .map((item: Attachment) => {
                    const IconComp = getFileIcon(item.fileType ?? '');
                    const url = `${BASE_URL}/projects/${projectId}/attachments/${item.id}/content`;
                    const canPreview = isPreviewableType(item.fileType);
                    return (
                      <div key={item.id} className="flex items-center gap-2.5 px-3 py-2 bg-card rounded-md border border-border/40">
                        <IconComp className="h-4 w-4 text-muted-foreground shrink-0" strokeWidth={1.5} />
                        <button
                          className={`text-sm text-foreground truncate flex-1 text-left ${canPreview ? 'hover:underline cursor-pointer' : ''}`}
                          onClick={() => canPreview ? setEvidencePreview({ name: item.fileName, url, type: item.fileType || '' }) : undefined}
                        >
                          {item.fileName}
                        </button>
                        <span className="text-xs text-muted-foreground shrink-0">{formatFileSize(item.fileSize)}</span>
                        {canPreview && (
                          <button
                            onClick={() => setEvidencePreview({ name: item.fileName, url, type: item.fileType || '' })}
                            className="text-muted-foreground hover:text-foreground transition-colors shrink-0"
                            title="Preview"
                          >
                            <Eye className="h-3.5 w-3.5" strokeWidth={1.5} />
                          </button>
                        )}
                        <button
                          onClick={() => downloadWithAuth(url, item.fileName)}
                          className="text-muted-foreground hover:text-foreground transition-colors shrink-0"
                          title="Download"
                        >
                          <Download className="h-3.5 w-3.5" strokeWidth={1.5} />
                        </button>
                        {!isReadOnly && (
                          <button
                            onClick={async () => {
                              try {
                                await attachmentsApi.delete(projectId!, item.id);
                                qc.invalidateQueries({ queryKey: ['attachments', projectId!, 'case', activeCase.id] });
                                toast.success('File removed');
                              } catch {
                                toast.error('Failed to remove file');
                              }
                            }}
                            className="p-1 rounded hover:bg-muted text-muted-foreground hover:text-destructive transition-colors shrink-0"
                          >
                            <Trash2 className="h-3.5 w-3.5" strokeWidth={1.5} />
                          </button>
                        )}
                      </div>
                    );
                  })}
              </div>
            </div>
          )}

          {stepsLoading ? (
            <div className="flex items-center justify-center py-12 gap-2 text-muted-foreground">
              <Loader2 className="h-4 w-4 animate-spin" />
              <span className="text-sm">Loading steps…</span>
            </div>
          ) : steps.length > 0 ? (
            <div className="bg-card rounded-lg shadow-soft overflow-hidden">
              <table className="w-full text-sm">
                <thead>
                  <tr className="surface-low">
                    <th className="px-4 py-2.5 text-left font-medium text-muted-foreground text-xs uppercase tracking-wider w-10">#</th>
                    <th className="px-4 py-2.5 text-left font-medium text-muted-foreground text-xs uppercase tracking-wider">Step</th>
                    <th className="px-4 py-2.5 text-left font-medium text-muted-foreground text-xs uppercase tracking-wider">Expected result</th>
                    <th className="px-4 py-2.5 text-center font-medium text-muted-foreground text-xs uppercase tracking-wider w-56">Status</th>
                    <th className="px-4 py-2.5 text-center font-medium text-muted-foreground text-xs uppercase tracking-wider w-px whitespace-nowrap">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {sortedSteps.map((step, sIdx) => {
                    const currentStatus =
                      (stepStatuses[activeCase.id] || [])[sIdx]
                      ?? persistedStepStatusByKey[`${activeCase.id}::${step.stepNumber}`]
                      ?? 'untested';
                    const totalEvidenceCount = stepEvidenceCounts[`${activeCase.id}::${step.stepNumber}`] ?? 0;
                    const stepDefectCount = createdDefects.filter(
                      (d) => d.caseId === activeCase.id && d.stepIdx === sIdx,
                    ).length;
                    return (
                      <tr key={sIdx} className="ghost-border border-t">
                        <td className="px-4 py-3 text-muted-foreground font-mono text-xs">{step.stepNumber}</td>
                        <td className="px-4 py-3 text-foreground">{step.action}</td>
                        <td className="px-4 py-3 text-foreground">{step.expectedResult}</td>
                        <td className="px-4 py-3">
                          <div className="flex items-center justify-center gap-1">
                            {(['untested', 'passed', 'failed', 'skipped'] as StepStatus[]).map((s) => (
                              <button
                                key={s}
                                onClick={() => setStepStatus(activeCase.id, step.stepNumber, step.id, s)}
                                disabled={isReadOnly}
                                className={`px-2.5 py-1 rounded-md text-[10px] font-mono font-medium uppercase tracking-widest transition-colors ${
                                  currentStatus === s ? statusColors[s] : statusOutline[s]
                                } ${isReadOnly ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}`}
                              >
                                {s}
                              </button>
                            ))}
                          </div>
                        </td>
                        <td className="px-4 py-3 w-px whitespace-nowrap">
                          <div className="flex items-center justify-center gap-1">
                            {/* Paperclip - Evidence */}
                            <div className="relative">
                              <Button
                                variant="ghost"
                                size="icon"
                                className={`h-7 w-7 ${totalEvidenceCount > 0 ? 'text-primary' : 'text-muted-foreground'} ${isReadOnly ? 'opacity-50 cursor-not-allowed' : ''}`}
                                onClick={() => handleEvidenceClick(activeCase.id, step.stepNumber - 1)}
                                disabled={isReadOnly}
                              >
                                <Paperclip className="h-3.5 w-3.5" strokeWidth={1.5} />
                              </Button>
                              {totalEvidenceCount > 0 && (
                                <span data-testid="step-evidence-badge" className="absolute -top-1 -right-1 h-4 w-4 rounded-full bg-primary text-primary-foreground text-[9px] font-bold flex items-center justify-center">
                                  {totalEvidenceCount}
                                </span>
                              )}
                            </div>
                            {/* Bug - Defect */}
                            <div className="relative">
                              <Button
                                data-testid="step-bug-btn"
                                variant="ghost"
                                size="icon"
                                className={`h-7 w-7 ${stepDefectCount > 0 ? 'text-destructive' : 'text-muted-foreground hover:text-destructive'} ${isReadOnly ? 'opacity-50 cursor-not-allowed' : ''}`}
                                onClick={() => handleBugClick(activeCase.id, step.stepNumber - 1)}
                                disabled={isReadOnly}
                              >
                                <Bug className="h-3.5 w-3.5" strokeWidth={1.5} />
                              </Button>
                              {stepDefectCount > 0 && (
                                <span data-testid="step-defect-badge" className="absolute -top-1 -right-1 h-4 w-4 rounded-full bg-destructive text-destructive-foreground text-[9px] font-bold flex items-center justify-center">
                                  {stepDefectCount}
                                </span>
                              )}
                            </div>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="text-center py-12 text-muted-foreground text-sm">No steps defined for this case.</div>
          )}

          {/* Defects Section - Inside center panel */}
          {activeCaseDefects.length > 0 && (
            <div className="mt-6 bg-card rounded-lg shadow-soft overflow-hidden">
              <table className="w-full text-sm">
                <thead>
                  <tr className="bg-slate-200 dark:bg-slate-700 sticky top-0 z-10">
                    <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">ID</th>
                    <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Title</th>
                    <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Severity</th>
                    <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Status</th>
                    <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Source</th>
                    <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider">Created</th>
                    <th className="text-left px-5 py-3 font-semibold text-slate-700 dark:text-slate-200 text-xs uppercase tracking-wider w-px whitespace-nowrap">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {activeCaseDefects.map((d) => (
                    <tr
                      key={d.id}
                      className="hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-colors border-b border-slate-100 dark:border-slate-700/50 bg-card cursor-pointer"
                      onClick={() => openViewDefect(d)}
                    >
                      <td className="px-5 py-3.5 font-mono text-[10px] text-accent tracking-wider">{d.displayId}</td>
                      <td className="px-5 py-3.5 font-medium text-foreground">{d.title}</td>
                      <td className="px-5 py-3.5" onClick={(e) => e.stopPropagation()}>
                        <Select value={d.severity} onValueChange={(val) => {
                          const updated = createdDefects.map(x => x.id === d.id ? { ...x, severity: val as any } : x);
                          setCreatedDefects(updated);
                        }}>
                          <SelectTrigger className={`h-7 w-auto text-[10px] font-mono uppercase tracking-widest rounded-md px-2.5 ${severityBadge[d.severity] || ''}`}>
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="Critical">Critical</SelectItem>
                            <SelectItem value="Major">Major</SelectItem>
                            <SelectItem value="Minor">Minor</SelectItem>
                          </SelectContent>
                        </Select>
                      </td>
                      <td className="px-5 py-3.5" onClick={(e) => e.stopPropagation()}>
                        <Select value={d.status} onValueChange={(val) => {
                          const updated = createdDefects.map(x => x.id === d.id ? { ...x, status: val as any } : x);
                          setCreatedDefects(updated);
                        }}>
                          <SelectTrigger className={`h-7 w-auto text-[10px] font-mono uppercase tracking-widest rounded-md px-2.5 ${statusBadge[d.status] || ''}`}>
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="Open">Open</SelectItem>
                            <SelectItem value="In Progress">In Progress</SelectItem>
                            <SelectItem value="Fixed">Fixed</SelectItem>
                            <SelectItem value="Closed">Closed</SelectItem>
                          </SelectContent>
                        </Select>
                      </td>
                      <td className="px-5 py-3.5 font-mono text-[10px] text-muted-foreground tracking-wider">
                        {getCaseSourceLabel(d.caseId)}{d.stepIdx !== undefined ? ` · Step ${d.stepIdx + 1}` : ''}
                      </td>
                      <td className="px-5 py-3.5 text-muted-foreground font-mono text-[10px] tracking-wider">{d.createdAt}</td>
                      <td className="px-5 py-3.5 w-px whitespace-nowrap" onClick={(e) => e.stopPropagation()}>
                        <div className="flex items-center gap-1">
                          <button
                            data-testid="defect-view-btn"
                            onClick={() => openViewDefect(d)}
                            className="p-1.5 rounded hover:bg-muted text-muted-foreground hover:text-foreground transition-colors"
                          >
                            <Eye className="h-3.5 w-3.5" strokeWidth={1.5} />
                          </button>
                          <button
                            data-testid="defect-edit-btn"
                            onClick={() => openEditDefect(d)}
                            className="p-1.5 rounded hover:bg-muted text-muted-foreground hover:text-foreground transition-colors"
                          >
                            <Pencil className="h-3.5 w-3.5" strokeWidth={1.5} />
                          </button>
                          <button
                            data-testid="defect-delete-btn"
                            onClick={() => {
                              setCreatedDefects(createdDefects.filter(x => x.id !== d.id));
                              toast.success('Defect deleted');
                            }}
                            className="p-1.5 rounded hover:bg-muted text-muted-foreground hover:text-destructive transition-colors"
                          >
                            <Trash2 className="h-3.5 w-3.5" strokeWidth={1.5} />
                          </button>
                          {d.link && (
                            <a
                              href={d.link}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="p-1.5 rounded hover:bg-muted text-muted-foreground hover:text-foreground transition-colors"
                            >
                              <ExternalLink className="h-3.5 w-3.5" strokeWidth={1.5} />
                            </a>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* Evidence Upload Modal */}
      <Dialog open={evidenceModalOpen} onOpenChange={setEvidenceModalOpen}>
        <DialogContent className="sm:max-w-md bg-card">
          <DialogHeader>
            <DialogTitle className="text-foreground">Upload Evidence</DialogTitle>
            <DialogDescription className="text-muted-foreground">
              Attach screenshots, logs, or files for{' '}
              {evidenceTarget ? `Step ${(evidenceTarget.stepIdx + 1)}` : ''} of case{' '}
              {evidenceTarget ? getCaseSourceLabel(evidenceTarget.caseId) : ''}.
            </DialogDescription>
          </DialogHeader>

          <div
            className="border-2 border-dashed border-border rounded-lg p-8 text-center cursor-pointer hover:border-primary/50 hover:bg-accent/5 transition-colors"
            onClick={() => fileInputRef.current?.click()}
            onDragOver={(e) => { e.preventDefault(); e.stopPropagation(); }}
            onDrop={(e) => {
              e.preventDefault();
              e.stopPropagation();
              handleFileUpload(e.dataTransfer.files);
            }}
          >
            <Upload className="h-8 w-8 text-muted-foreground mx-auto mb-3" strokeWidth={1.5} />
            <p className="text-sm text-foreground font-medium mb-1">Drop files here or click to browse</p>
            <p className="text-xs text-muted-foreground">Screenshots, logs, PDFs, or any relevant files</p>
            <input
              ref={fileInputRef}
              type="file"
              multiple
              className="hidden"
              onChange={(e) => handleFileUpload(e.target.files)}
            />
          </div>

          {evidenceTarget && (() => {
            const inMemory = getEvidenceFiles(evidenceTarget.caseId, evidenceTarget.stepIdx);
            const key = `${evidenceTarget.caseId}::${evidenceTarget.stepIdx}`;
            const fromServer = (serverEvidence[key] || []).filter(
              srv => !inMemory.some(m => m.name === srv.fileName && m.size === srv.fileSize)
            );
            const allFiles = [...inMemory, ...fromServer];
            if (allFiles.length === 0) return null;
            return (
              <div className="mt-3 space-y-1.5">
                <p className="text-xs font-semibold text-foreground uppercase tracking-wider">Uploaded Files</p>
                {inMemory.map((file, i) => {
                  const IconComp = getFileIcon(file.type);
                  const canPreview = isPreviewableType(file.type);
                  return (
                    <div key={`mem-${i}`} className="flex items-center gap-2.5 px-3 py-2 bg-secondary rounded-md">
                      <IconComp className="h-4 w-4 text-muted-foreground shrink-0" strokeWidth={1.5} />
                      <button
                        className={`text-sm text-foreground truncate flex-1 text-left ${canPreview ? 'hover:underline cursor-pointer' : ''}`}
                        onClick={() => {
                          if (!canPreview) return;
                          const url = URL.createObjectURL(file.file);
                          setEvidencePreview({ name: file.name, url, type: file.type, isLocal: true });
                        }}
                      >
                        {file.name}
                      </button>
                      <span className="text-xs text-muted-foreground shrink-0">{formatFileSize(file.size)}</span>
                      {canPreview && (
                        <button
                          onClick={() => {
                            const url = URL.createObjectURL(file.file);
                            setEvidencePreview({ name: file.name, url, type: file.type, isLocal: true });
                          }}
                          className="text-muted-foreground hover:text-foreground transition-colors"
                          title="Preview"
                        >
                          <Eye className="h-3.5 w-3.5" strokeWidth={1.5} />
                        </button>
                      )}
                    </div>
                  );
                })}
                {fromServer.map((att) => {
                  const IconComp = getFileIcon(att.fileType || '');
                  const url = `${BASE_URL}/projects/${projectId}/attachments/${att.id}/content`;
                  const canPreview = isPreviewableType(att.fileType);
                  return (
                    <div key={att.id} className="flex items-center gap-2.5 px-3 py-2 bg-secondary rounded-md">
                      <IconComp className="h-4 w-4 text-muted-foreground shrink-0" strokeWidth={1.5} />
                      <button
                        className={`text-sm text-foreground truncate flex-1 text-left ${canPreview ? 'hover:underline cursor-pointer' : ''}`}
                        onClick={() => canPreview ? setEvidencePreview({ name: att.fileName, url, type: att.fileType || '' }) : undefined}
                      >
                        {att.fileName}
                      </button>
                      <span className="text-xs text-muted-foreground shrink-0">{formatFileSize(att.fileSize)}</span>
                      {canPreview && (
                        <button
                          onClick={() => setEvidencePreview({ name: att.fileName, url, type: att.fileType || '' })}
                          className="text-muted-foreground hover:text-foreground transition-colors"
                          title="Preview"
                        >
                          <Eye className="h-3.5 w-3.5" strokeWidth={1.5} />
                        </button>
                      )}
                      <button
                        onClick={() => downloadWithAuth(url, att.fileName)}
                        className="text-muted-foreground hover:text-foreground transition-colors"
                        title="Download"
                      >
                        <Download className="h-3.5 w-3.5" strokeWidth={1.5} />
                      </button>
                    </div>
                  );
                })}
              </div>
            );
          })()}

          <div className="flex justify-end mt-4">
            <Button variant="ghost" onClick={() => setEvidenceModalOpen(false)}>Done</Button>
          </div>
        </DialogContent>
      </Dialog>

      {/* Evidence / Case Attachment Image Preview */}
      <Dialog open={!!evidencePreview} onOpenChange={(open) => { if (!open) closeEvidencePreview(); }}>
        <DialogContent className="sm:max-w-2xl bg-background">
          <DialogHeader>
            <DialogTitle className="text-base font-semibold truncate">{evidencePreview?.name}</DialogTitle>
            <DialogDescription className="sr-only">File preview</DialogDescription>
          </DialogHeader>
          <div className="flex items-center justify-center min-h-[300px] bg-muted/20 rounded-lg overflow-hidden">
            {evidencePreview && isPdfType(evidencePreview.type) ? (
              evidencePreview.isLocal
                ? <iframe src={evidencePreview.url} className="w-full h-[60vh]" title="PDF preview" />
                : <AuthenticatedPdf url={evidencePreview.url} className="w-full h-[60vh]" />
            ) : evidencePreview && evidencePreview.isLocal ? (
              <img src={evidencePreview.url} alt={evidencePreview.name} className="max-w-full max-h-[60vh] object-contain" />
            ) : evidencePreview ? (
              <AuthenticatedImage url={evidencePreview.url} alt={evidencePreview.name} className="max-w-full max-h-[60vh] object-contain" />
            ) : null}
          </div>
          <div className="flex justify-end gap-2 mt-2">
            <Button variant="ghost" onClick={closeEvidencePreview}>Close</Button>
            {evidencePreview && (
              <Button className="gap-1.5" onClick={() => {
                if (evidencePreview.isLocal) {
                  const a = document.createElement('a');
                  a.href = evidencePreview.url;
                  a.download = evidencePreview.name;
                  a.click();
                } else {
                  downloadWithAuth(evidencePreview.url, evidencePreview.name);
                }
              }}>
                <Download className="h-3.5 w-3.5" /> Download
              </Button>
            )}
          </div>
        </DialogContent>
      </Dialog>

      {/* Create Defect Modal */}
      {defectContext && (
        <CreateDefectModal
          open={defectModalOpen}
          onOpenChange={setDefectModalOpen}
          caseId={defectContext.caseId}
          caseTitle={activeCase.title}
          stepIdx={defectContext.stepIdx}
          sourceInitial={defectContext.source}
          onCreateDefect={handleCreateDefect}
        />
      )}

      {/* View Defect Modal */}
      <ViewDefectModal
        open={viewDefectOpen}
        onOpenChange={setViewDefectOpen}
        defect={viewDefect as any}
        displayId={viewDefect?.displayId}
        existingAttachments={viewDefectAttachments}
        formatDate={formatDate}
        projectId={projectId!}
      />

      {/* Edit Defect Modal */}
      <EditDefectModal
        open={editDefectOpen}
        onOpenChange={setEditDefectOpen}
        defect={editDefect as any}
        displayId={editDefect?.displayId}
        existingAttachments={editDefectAttachments}
        onSave={handleEditDefectSave}
        projectId={projectId!}
      />
    </div>
  );
};

export default RunExecution;
