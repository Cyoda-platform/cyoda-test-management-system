import { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, FolderOpen, Play, FlaskConical } from 'lucide-react';
import { mockProjects, mockSuites, mockTestRuns } from '@/data/mockData';

const HighlightMatch = ({ text, query }: { text: string; query: string }) => {
  if (!query.trim()) return <>{text}</>;
  const idx = text.toLowerCase().indexOf(query.toLowerCase());
  if (idx === -1) return <>{text}</>;
  return (
    <>
      {text.slice(0, idx)}
      <span className="text-purple-600 font-semibold">{text.slice(idx, idx + query.length)}</span>
      {text.slice(idx + query.length)}
    </>
  );
};

const buildSnippet = (text: string, query: string, maxLen = 45, prefix?: string): string | null => {
  if (!text || !query.trim()) return null;
  const idx = text.toLowerCase().indexOf(query.toLowerCase());
  if (idx === -1) return null;
  const matchEnd = idx + query.length;
  const half = Math.floor((maxLen - query.length) / 2);
  const start = Math.max(0, idx - half);
  const end = Math.min(text.length, matchEnd + half);
  let snippet = '';
  if (start > 0) snippet += '…';
  snippet += text.slice(start, end);
  if (end < text.length) snippet += '…';
  if (prefix) snippet = `${prefix}: ${snippet}`;
  return snippet.slice(0, maxLen + (prefix ? prefix.length + 2 : 0) + 2);
};

const SnippetHighlight = ({ snippet, query }: { snippet: string; query: string }) => {
  if (!query.trim()) return <>{snippet}</>;
  const idx = snippet.toLowerCase().indexOf(query.toLowerCase());
  if (idx === -1) return <>{snippet}</>;
  return (
    <>
      {snippet.slice(0, idx)}
      <span className="text-purple-600 font-medium">{snippet.slice(idx, idx + query.length)}</span>
      {snippet.slice(idx + query.length)}
    </>
  );
};

interface CaseWithMeta {
  id: string;
  suiteId: string;
  title: string;
  priority: string;
  description: string;
  preconditions: string;
  steps: { order: number; action: string; expectedResult: string; status: string }[];
  deleted: boolean;
  suiteName: string;
  projectId: string;
}

const getCaseSnippet = (c: CaseWithMeta, q: string): string | null => {
  if (c.title.toLowerCase().includes(q) || c.id.toLowerCase().includes(q)) {
    // Match is in title/ID — only show snippet if ALSO in desc/steps
  }
  const titleMatch = c.title.toLowerCase().includes(q);
  const idMatch = c.id.toLowerCase().includes(q);
  if (titleMatch || idMatch) {
    // Don't show snippet for title/ID matches unless also in desc/steps
    const descSnippet = buildSnippet(c.description, q);
    if (descSnippet) return descSnippet;
    for (const step of c.steps) {
      const actionSnippet = buildSnippet(step.action, q, 45, `Step ${step.order}`);
      if (actionSnippet) return actionSnippet;
      const erSnippet = buildSnippet(step.expectedResult, q, 45, `Step ${step.order}`);
      if (erSnippet) return erSnippet;
    }
    return null;
  }
  // Match is in description or steps
  const descSnippet = buildSnippet(c.description, q);
  if (descSnippet) return descSnippet;
  for (const step of c.steps) {
    const actionSnippet = buildSnippet(step.action, q, 45, `Step ${step.order}`);
    if (actionSnippet) return actionSnippet;
    const erSnippet = buildSnippet(step.expectedResult, q, 45, `Step ${step.order}`);
    if (erSnippet) return erSnippet;
  }
  return null;
};

const getProjectSnippet = (p: { name: string; description: string }, q: string): string | null => {
  if (p.name.toLowerCase().includes(q)) return null;
  return buildSnippet(p.description, q);
};

const GlobalSearch = () => {
  const [query, setQuery] = useState('');
  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);
  const navigate = useNavigate();
  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const allCases = mockSuites.flatMap((suite) =>
    suite.cases
      .filter((c) => !c.deleted)
      .map((c) => ({ ...c, suiteName: suite.name, projectId: suite.projectId }))
  );

  const q = query.toLowerCase().trim();

  const filteredProjects = q
    ? mockProjects.filter(
        (p) => p.name.toLowerCase().includes(q) || p.description.toLowerCase().includes(q)
      )
    : [];

  const filteredCases = q
    ? allCases.filter(
        (c) =>
          c.id.toLowerCase().includes(q) ||
          c.title.toLowerCase().includes(q) ||
          c.description.toLowerCase().includes(q) ||
          c.steps.some(
            (s) =>
              s.action.toLowerCase().includes(q) ||
              s.expectedResult.toLowerCase().includes(q)
          )
      )
    : [];

  const filteredRuns = q
    ? mockTestRuns.filter(
        (r) => r.id.toLowerCase().includes(q) || r.name.toLowerCase().includes(q)
      )
    : [];

  const allResults = [
    ...filteredProjects.map((p) => ({ type: 'project' as const, id: p.id, item: p })),
    ...filteredCases.map((c) => ({ type: 'case' as const, id: c.id, item: c })),
    ...filteredRuns.map((r) => ({ type: 'run' as const, id: r.id, item: r })),
  ];

  const hasResults = allResults.length > 0;
  const showDropdown = open && q.length > 0;

  const handleSelect = useCallback(
    (result: (typeof allResults)[0]) => {
      setOpen(false);
      setQuery('');
      inputRef.current?.blur();
      if (result.type === 'project') {
        navigate(`/projects/${result.id}/repository`);
      } else if (result.type === 'case') {
        navigate(`/projects/${(result.item as any).projectId}/repository`);
      } else {
        navigate(`/projects/${(result.item as any).projectId}/runs/${result.id}`);
      }
    },
    [navigate]
  );

  useEffect(() => {
    setActiveIndex(-1);
  }, [query]);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (!showDropdown || !hasResults) return;
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setActiveIndex((i) => (i < allResults.length - 1 ? i + 1 : 0));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setActiveIndex((i) => (i > 0 ? i - 1 : allResults.length - 1));
    } else if (e.key === 'Enter' && activeIndex >= 0) {
      e.preventDefault();
      handleSelect(allResults[activeIndex]);
    } else if (e.key === 'Escape') {
      setOpen(false);
    }
  };

  let currentIdx = -1;

  return (
    <div ref={containerRef} className="relative">
      <div className="flex items-center gap-2 h-8 w-96 rounded-md bg-white/[0.08] border border-white/[0.06] px-3">
        <Search className="h-3.5 w-3.5 shrink-0 text-white/40" strokeWidth={1.5} />
        <input
          ref={inputRef}
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setOpen(true);
          }}
          onFocus={() => setOpen(true)}
          onKeyDown={handleKeyDown}
          placeholder="Search"
          className="flex-1 bg-transparent text-xs text-white/90 placeholder:text-white/40 outline-none"
        />
      </div>

      {showDropdown && (
        <div className="absolute top-full left-0 mt-1.5 w-96 max-h-[400px] overflow-y-auto rounded-lg border border-border bg-white shadow-xl z-[100]">
          {!hasResults ? (
            <p className="py-6 text-center text-sm text-muted-foreground">No results found.</p>
          ) : (
            <>
              {filteredProjects.length > 0 && (
                <div className="p-1">
                  <p className="px-2.5 py-1.5 text-[10px] font-medium uppercase tracking-wider text-muted-foreground">
                    Projects
                  </p>
                  {filteredProjects.map((p) => {
                    currentIdx++;
                    const idx = currentIdx;
                    const snippet = getProjectSnippet(p, q);
                    return (
                      <button
                        key={p.id}
                        onClick={() => handleSelect({ type: 'project', id: p.id, item: p })}
                        className={`w-full flex items-center gap-2.5 rounded-md px-2.5 py-2.5 text-left text-sm transition-colors ${
                          idx === activeIndex
                            ? 'bg-accent text-accent-foreground'
                            : 'hover:bg-muted/50'
                        }`}
                      >
                        <FolderOpen className="h-4 w-4 shrink-0 text-muted-foreground" strokeWidth={1.5} />
                        <div className="min-w-0 flex-1">
                          <span className="font-medium text-foreground"><HighlightMatch text={p.name} query={q} /></span>
                          <span className="ml-2 text-xs text-muted-foreground font-mono"><HighlightMatch text={p.id} query={q} /></span>
                          {snippet && (
                            <p className="text-[11px] text-muted-foreground/70 truncate mt-0.5">
                              <SnippetHighlight snippet={snippet} query={q} />
                            </p>
                          )}
                        </div>
                      </button>
                    );
                  })}
                </div>
              )}

              {filteredCases.length > 0 && (
                <div className="p-1 border-t border-border">
                  <p className="px-2.5 py-1.5 text-[10px] font-medium uppercase tracking-wider text-muted-foreground">
                    Test Cases
                  </p>
                  {filteredCases.map((c) => {
                    currentIdx++;
                    const idx = currentIdx;
                    const snippet = getCaseSnippet(c, q);
                    return (
                      <button
                        key={c.id}
                        onClick={() => handleSelect({ type: 'case', id: c.id, item: c })}
                        className={`w-full flex items-center gap-2.5 rounded-md px-2.5 py-2.5 text-left text-sm transition-colors ${
                          idx === activeIndex
                            ? 'bg-accent text-accent-foreground'
                            : 'hover:bg-muted/50'
                        }`}
                      >
                        <FlaskConical className="h-4 w-4 shrink-0 text-muted-foreground mt-0.5" strokeWidth={1.5} />
                        <div className="min-w-0 flex-1">
                          <span className="font-medium text-foreground"><HighlightMatch text={c.title} query={q} /></span>
                          <p className="text-xs text-muted-foreground truncate">
                            {c.suiteName} · <HighlightMatch text={c.id} query={q} />
                          </p>
                          {snippet && (
                            <p className="text-[11px] text-muted-foreground/70 truncate mt-0.5">
                              <SnippetHighlight snippet={snippet} query={q} />
                            </p>
                          )}
                        </div>
                      </button>
                    );
                  })}
                </div>
              )}

              {filteredRuns.length > 0 && (
                <div className="p-1 border-t border-border">
                  <p className="px-2.5 py-1.5 text-[10px] font-medium uppercase tracking-wider text-muted-foreground">
                    Test Runs
                  </p>
                  {filteredRuns.map((r) => {
                    currentIdx++;
                    const idx = currentIdx;
                    return (
                      <button
                        key={r.id}
                        onClick={() => handleSelect({ type: 'run', id: r.id, item: r })}
                        className={`w-full flex items-center gap-2.5 rounded-md px-2.5 py-2.5 text-left text-sm transition-colors ${
                          idx === activeIndex
                            ? 'bg-accent text-accent-foreground'
                            : 'hover:bg-muted/50'
                        }`}
                      >
                        <Play className="h-4 w-4 shrink-0 text-muted-foreground" strokeWidth={1.5} />
                        <div className="min-w-0 flex-1">
                          <span className="font-medium text-foreground"><HighlightMatch text={r.name} query={q} /></span>
                          <p className="text-xs text-muted-foreground">
                            {r.environment} · <HighlightMatch text={r.id} query={q} />
                          </p>
                        </div>
                      </button>
                    );
                  })}
                </div>
              )}
            </>
          )}
        </div>
      )}
    </div>
  );
};

export default GlobalSearch;
