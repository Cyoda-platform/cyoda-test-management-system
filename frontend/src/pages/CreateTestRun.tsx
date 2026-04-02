import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Breadcrumbs from '@/components/Breadcrumbs';
import { useQueries } from '@tanstack/react-query';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Checkbox } from '@/components/ui/checkbox';
import { ChevronDown, ChevronRight, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { useProject, useSuites, useCreateTestRun, keys } from '@/hooks/useApi';
import { testCasesApi } from '@/lib/api';
import { listDisplayId } from '@/lib/utils';

const labelCls = 'text-[10px] font-semibold text-muted-foreground uppercase mb-1.5 block font-mono tracking-widest';

const CreateTestRun = () => {
  const { projectId } = useParams<{ projectId: string }>();
  const navigate = useNavigate();

  // Live data
  const { data: project } = useProject(projectId!);
  const { data: suites = [], isLoading: suitesLoading } = useSuites(projectId!);
  const createTestRun = useCreateTestRun();

  // Fetch cases for every suite in parallel
  const casesQueries = useQueries({
    queries: suites.map((suite) => ({
      queryKey: keys.cases.all(projectId!, suite.id),
      queryFn:  () => testCasesApi.list(projectId!, suite.id),
      enabled:  !!projectId && suites.length > 0,
      select:   (res: { data: Array<{ id: string; title: string; priority: 'HIGH' | 'MEDIUM' | 'LOW'; suiteId: string; projectId: string; description: string; preconditions: string; deleted: boolean }> }) => res.data,
    })),
  });

  // Combine into suitesWithCases
  const suitesWithCases = suites.map((suite, i) => ({
    ...suite,
    cases: casesQueries[i]?.data ?? [],
  }));

  const casesLoading = casesQueries.some((q) => q.isLoading);

  const [runName, setRunName] = useState('');
  const [environment, setEnvironment] = useState('Staging');
  const [buildVersion, setBuildVersion] = useState('');
  const [description, setDescription] = useState('');
  const [selectedCases, setSelectedCases] = useState<Set<string>>(new Set());
  const [expandedSuites, setExpandedSuites] = useState<Set<string>>(new Set());
  const [priorityFilter, setPriorityFilter] = useState<Set<string>>(new Set(['HIGH', 'MEDIUM', 'LOW']));

  const toggleSuiteExpand = (suiteId: string) => {
    const next = new Set(expandedSuites);
    if (next.has(suiteId)) {
      next.delete(suiteId);
    } else {
      next.add(suiteId);
    }
    setExpandedSuites(next);
  };

  const toggleCaseSelect = (caseId: string) => {
    const next = new Set(selectedCases);
    if (next.has(caseId)) {
      next.delete(caseId);
    } else {
      next.add(caseId);
    }
    setSelectedCases(next);
  };

  const togglePriority = (priority: 'HIGH' | 'MEDIUM' | 'LOW') => {
    const next = new Set(priorityFilter);
    if (next.has(priority)) {
      next.delete(priority);
    } else {
      next.add(priority);
    }
    setPriorityFilter(next);
  };

  const handleCreate = () => {
    if (!runName.trim()) {
      toast.error('Run name is required');
      return;
    }
    if (selectedCases.size === 0) {
      toast.error('Please select at least one test case');
      return;
    }

    createTestRun.mutate(
      {
        projectId: projectId!,
        body: {
          name: runName,
          environment,
          buildVersion,
          description,
          status: 'initial',
          passed: 0,
          failed: 0,
          skipped: 0,
          untested: selectedCases.size,
        },
      },
      {
        onSuccess: (newRun) => {
          toast.success(`Test run "${runName}" created with ${selectedCases.size} cases`);
          navigate(`/projects/${projectId}/runs/${newRun.id}`);
        },
        onError: (e) => toast.error(e.message),
      }
    );
  };

  return (
    <div className="h-full flex flex-col">
      <div className="flex-1 overflow-auto">
        <div className="max-w-4xl mx-auto px-8 py-6 space-y-6">
          <Breadcrumbs segments={[
            { label: 'Projects', href: '/projects' },
            { label: project?.name || 'Project', href: `/projects/${projectId}/runs` },
            { label: 'Test Runs', href: `/projects/${projectId}/runs` },
            { label: 'Create Test Run' },
          ]} />

          <h1 className="text-xl font-bold text-foreground tracking-[-0.02em]">Create Test Run</h1>

          <div className="space-y-5">
            {/* Run Name */}
            <div>
              <label className={labelCls}>Run Name *</label>
              <Input
                value={runName}
                onChange={(e) => setRunName(e.target.value)}
                placeholder="e.g. Sprint 13 Regression"
                className="mt-0 h-9 bg-white border border-input focus-visible:ring-1 focus-visible:ring-ring"
              />
            </div>

            {/* Environment */}
            <div>
              <label className={labelCls}>Environment</label>
              <Select value={environment} onValueChange={setEnvironment}>
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

            {/* Build / Version */}
            <div>
              <label className={labelCls}>Build / Version</label>
              <Input
                value={buildVersion}
                onChange={(e) => setBuildVersion(e.target.value)}
                placeholder="e.g. v2.4.0-rc1"
                className="mt-0 h-9 bg-white border border-input focus-visible:ring-1 focus-visible:ring-ring"
              />
            </div>

            {/* Description */}
            <div>
              <label className={labelCls}>Description</label>
              <Textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Describe the purpose of this test run..."
                className="mt-0 bg-white border border-input focus-visible:ring-1 focus-visible:ring-ring min-h-[80px] resize-none"
              />
            </div>

            {/* Priority Filter */}
            <div>
              <label className={labelCls}>Priority Filter</label>
              <div className="flex items-center gap-4 mt-1.5">
                {(['HIGH', 'MEDIUM', 'LOW'] as const).map((p) => (
                  <label key={p} className="flex items-center gap-2 cursor-pointer">
                    <Checkbox
                      checked={priorityFilter.has(p)}
                      onCheckedChange={() => togglePriority(p)}
                    />
                    <span className="text-xs font-medium text-foreground">{p.charAt(0) + p.slice(1).toLowerCase()}</span>
                  </label>
                ))}
              </div>
            </div>

            {/* Select Test Cases */}
            <div className="space-y-2">
              <label className={labelCls}>
                Select Test Cases <span className="text-muted-foreground/60">({selectedCases.size} selected)</span>
              </label>
              <div className="border border-border rounded-lg bg-card divide-y divide-border/40 max-h-[400px] overflow-y-auto">
                {(suitesLoading || casesLoading) && (
                  <div className="flex items-center justify-center gap-2 py-8 text-muted-foreground">
                    <Loader2 className="h-4 w-4 animate-spin" />
                    <span className="text-sm">Loading test cases…</span>
                  </div>
                )}
                {!suitesLoading && suitesWithCases.map((suite) => {
                  const filteredCases = suite.cases.filter((c) => priorityFilter.has(c.priority));
                  const isExpanded = expandedSuites.has(suite.id);
                  const selectedInSuite = filteredCases.filter((c) => selectedCases.has(c.id)).length;

                  return (
                    <div key={suite.id}>
                      <div className="flex items-center gap-2 px-4 py-3 hover:bg-secondary/40 cursor-pointer transition-colors">
                        <button
                          onClick={() => toggleSuiteExpand(suite.id)}
                          className="shrink-0 text-muted-foreground hover:text-foreground"
                        >
                          {isExpanded ? (
                            <ChevronDown className="h-4 w-4" strokeWidth={1.5} />
                          ) : (
                            <ChevronRight className="h-4 w-4" strokeWidth={1.5} />
                          )}
                        </button>
                        <span className="text-sm font-semibold text-foreground flex-1">{suite.name}</span>
                        <span className="text-xs font-mono text-muted-foreground">
                          {selectedInSuite}/{filteredCases.length}
                        </span>
                      </div>

                      {isExpanded && (
                        <div className="divide-y divide-border/40">
                          {filteredCases.map((tc, index) => (
                            <div
                              key={tc.id}
                              onClick={() => toggleCaseSelect(tc.id)}
                              className="flex items-center gap-3 px-4 py-2.5 pl-12 hover:bg-secondary/40 cursor-pointer transition-colors"
                            >
                              <Checkbox
                                checked={selectedCases.has(tc.id)}
                                onCheckedChange={() => toggleCaseSelect(tc.id)}
                                onClick={(e) => e.stopPropagation()}
                              />
                              <span className="text-[10px] font-mono text-muted-foreground w-12 shrink-0" title={tc.id}>
                                {listDisplayId('TC', index)}
                              </span>
                              <span className="text-sm text-foreground flex-1 truncate">{tc.title}</span>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Footer */}
      <div className="shrink-0 border-t border-border bg-card px-8 py-3 flex items-center justify-end gap-3">
        <Button variant="ghost" size="sm" onClick={() => navigate(`/projects/${projectId}/runs`)}>
          Cancel
        </Button>
        <Button
          size="sm"
          className="bg-primary text-primary-foreground hover:bg-primary/90 border-0 px-6"
          disabled={!runName.trim() || selectedCases.size === 0 || createTestRun.isPending}
          onClick={handleCreate}
        >
          {createTestRun.isPending
            ? <><Loader2 className="h-3.5 w-3.5 animate-spin mr-1.5" />Creating…</>
            : `Create Run (${selectedCases.size} cases)`}
        </Button>
      </div>
    </div>
  );
};

export default CreateTestRun;

