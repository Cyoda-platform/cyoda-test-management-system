import { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, FolderOpen, Loader2, FileText, CheckSquare, AlertCircle, BarChart3 } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { searchApi, SearchResult, GlobalSearchResponse } from '@/lib/api';

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

const buildSnippet = (text: string, query: string, maxLen = 45): string | null => {
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
  return snippet.slice(0, maxLen + 2);
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

const getIconForType = (type: string) => {
  const className = "h-4 w-4 shrink-0 text-muted-foreground";
  switch (type) {
    case 'project':
      return <FolderOpen className={className} strokeWidth={1.5} />;
    case 'suite':
      return <FileText className={className} strokeWidth={1.5} />;
    case 'testcase':
      return <CheckSquare className={className} strokeWidth={1.5} />;
    case 'testrun':
      return <BarChart3 className={className} strokeWidth={1.5} />;
    case 'defect':
      return <AlertCircle className={className} strokeWidth={1.5} />;
    case 'report':
      return <BarChart3 className={className} strokeWidth={1.5} />;
    default:
      return <Search className={className} strokeWidth={1.5} />;
  }
};

const getTypeLabel = (type: string) => {
  switch (type) {
    case 'project':
      return 'Project';
    case 'suite':
      return 'Suite';
    case 'testcase':
      return 'Test Case';
    case 'testrun':
      return 'Test Run';
    case 'testruncases':
      return 'Run Case';
    case 'testrunchstep':
      return 'Step';
    case 'defect':
      return 'Defect';
    case 'report':
      return 'Report';
    default:
      return type;
  }
};

const GlobalSearch = () => {
  const [query, setQuery] = useState('');
  const [debouncedQuery, setDebouncedQuery] = useState('');
  const [open, setOpen] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);
  const navigate = useNavigate();
  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  // Debounce query by 300 ms to avoid hammering the API on every keystroke
  useEffect(() => {
    const timer = setTimeout(() => setDebouncedQuery(query), 300);
    return () => clearTimeout(timer);
  }, [query]);

  const trimmed = debouncedQuery.trim();

  const { data: searchResponse, isFetching } = useQuery({
    queryKey: ['global-search', trimmed],
    queryFn: () => searchApi.global(trimmed, 0, 10),
    enabled: trimmed.length >= 2,
    staleTime: 10_000,
  });

  const allResults = (searchResponse?.results ?? []).map((result) => ({
    type: result.type,
    id: result.id,
    item: result,
  }));

  const hasResults = allResults.length > 0;
  const showDropdown = open && query.length > 0;

  const handleSelect = useCallback(
    (result: (typeof allResults)[0]) => {
      setOpen(false);
      setQuery('');
      setDebouncedQuery('');
      inputRef.current?.blur();

      const searchResult = result.item as SearchResult;
      const projectId = searchResult.parentProjectId || result.id;

      // Navigate based on result type
      switch (result.type) {
        case 'project':
          navigate(`/projects/${result.id}/repository`);
          break;
        case 'suite':
          navigate(`/projects/${projectId}/suites/${result.id}`);
          break;
        case 'testcase':
          navigate(`/projects/${projectId}/testcases/${result.id}`);
          break;
        case 'defect':
          navigate(`/projects/${projectId}/defects/${result.id}`);
          break;
        case 'report':
          navigate(`/projects/${projectId}/reports/${result.id}`);
          break;
        case 'testrun':
          navigate(`/projects/${projectId}/testruns/${result.id}`);
          break;
        default:
          // For other types, navigate to project
          navigate(`/projects/${projectId}/repository`);
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

  return (
    <div ref={containerRef} className="relative">
      <div className="flex items-center gap-2 h-8 w-96 rounded-md bg-white/[0.08] border border-white/[0.06] px-3">
        {isFetching ? (
          <Loader2 className="h-3.5 w-3.5 shrink-0 text-white/40 animate-spin" strokeWidth={1.5} />
        ) : (
          <Search className="h-3.5 w-3.5 shrink-0 text-white/40" strokeWidth={1.5} />
        )}
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
          {trimmed.length < 2 ? (
            <p className="py-6 text-center text-sm text-muted-foreground">
              Type at least 2 characters to search.
            </p>
          ) : isFetching ? (
            <p className="py-6 text-center text-sm text-muted-foreground">Searching…</p>
          ) : !hasResults ? (
            <p className="py-6 text-center text-sm text-muted-foreground">No results found.</p>
          ) : (
            <div className="p-1">
              {allResults.map((result, idx) => {
                const searchResult = result.item as SearchResult;
                const icon = getIconForType(result.type);
                const typeLabel = getTypeLabel(result.type);
                const snippet = buildSnippet(searchResult.description || '', query);

                return (
                  <button
                    key={`${result.type}-${result.id}`}
                    onClick={() => handleSelect(result)}
                    className={`w-full flex items-center gap-2.5 rounded-md px-2.5 py-2.5 text-left text-sm transition-colors ${
                      idx === activeIndex
                        ? 'bg-accent text-accent-foreground'
                        : 'hover:bg-muted/50'
                    }`}
                  >
                    {icon}
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2">
                        <span className="font-medium text-foreground">
                          <HighlightMatch text={searchResult.title} query={query} />
                        </span>
                        <span className="text-[10px] px-1.5 py-0.5 bg-muted rounded text-muted-foreground">
                          {typeLabel}
                        </span>
                      </div>
                      {searchResult.metadata && (
                        <p className="text-xs text-muted-foreground mt-0.5">
                          {searchResult.metadata}
                        </p>
                      )}
                      {snippet && (
                        <p className="text-[11px] text-muted-foreground/70 truncate mt-0.5">
                          <SnippetHighlight snippet={snippet} query={query} />
                        </p>
                      )}
                    </div>
                  </button>
                );
              })}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default GlobalSearch;
