import { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, FolderOpen, Loader2 } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { projectsApi, Project } from '@/lib/api';

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

  const { data: projectResults = [], isFetching } = useQuery({
    queryKey: ['global-search-projects', trimmed],
    queryFn: () => projectsApi.search(trimmed),
    enabled: trimmed.length >= 2,
    staleTime: 10_000,
  });

  const allResults = (projectResults as Project[]).map((p) => ({
    type: 'project' as const,
    id: p.id,
    item: p,
  }));

  const hasResults = allResults.length > 0;
  const showDropdown = open && query.length > 0;

  const handleSelect = useCallback(
    (result: (typeof allResults)[0]) => {
      setOpen(false);
      setQuery('');
      setDebouncedQuery('');
      inputRef.current?.blur();
      navigate(`/projects/${result.id}/repository`);
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
          placeholder="Search projects"
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
              <p className="px-2.5 py-1.5 text-[10px] font-medium uppercase tracking-wider text-muted-foreground">
                Projects
              </p>
              {(projectResults as Project[]).map((p, idx) => {
                const q = trimmed;
                const snippet = p.name.toLowerCase().includes(q)
                  ? buildSnippet(p.description, query)
                  : null;
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
                      <span className="font-medium text-foreground">
                        <HighlightMatch text={p.name} query={query} />
                      </span>
                      <span className="ml-2 text-xs text-muted-foreground font-mono">
                        <HighlightMatch text={p.id} query={query} />
                      </span>
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
