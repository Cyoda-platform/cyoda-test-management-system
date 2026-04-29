/**
 * Minimal virtual list hook — no external dependencies needed.
 *
 * Accepts an HTMLElement (or null) directly so the effect re-runs whenever the
 * container element is assigned, even when it appears after an initial loading state.
 *
 * Usage:
 *   const [el, setEl] = useState<HTMLDivElement | null>(null);
 *   const { virtualItems, totalSize, scrollToIndex } = useVirtualList(el, {
 *     count: items.length,
 *     estimateSize: (i) => 44,
 *   });
 *
 *   return (
 *     <div ref={setEl} style={{ overflow: 'auto', height: '100%' }}>
 *       <div style={{ height: totalSize, position: 'relative' }}>
 *         {virtualItems.map(vi => (
 *           <div key={vi.key} style={{ position: 'absolute', top: vi.start, width: '100%', height: vi.size }}>
 *             {items[vi.index]}
 *           </div>
 *         ))}
 *       </div>
 *     </div>
 *   );
 */

import { useState, useCallback, useEffect, useMemo } from 'react';

export interface VirtualItem {
  index: number;
  /** Stable key for React reconciliation */
  key: number;
  /** Distance from top of the scroll container in px */
  start: number;
  /** Height of this item in px */
  size: number;
}

export interface UseVirtualListOptions {
  count: number;
  /** Return the estimated height (px) for item at `index`. */
  estimateSize: (index: number) => number;
  /** Extra items to render beyond the visible window on each side. Default: 5 */
  overscan?: number;
}

export function useVirtualList(
  containerEl: HTMLElement | null,
  options: UseVirtualListOptions,
) {
  const { count, estimateSize, overscan = 5 } = options;

  const [scrollTop, setScrollTop] = useState(0);
  const [containerHeight, setContainerHeight] = useState(600);

  // Re-runs whenever containerEl changes (null → element when loading skeleton disappears).
  useEffect(() => {
    if (!containerEl) return;

    const onScroll = () => setScrollTop(containerEl.scrollTop);
    const measure = () => setContainerHeight(containerEl.clientHeight);
    const ro = new ResizeObserver(measure);

    containerEl.addEventListener('scroll', onScroll, { passive: true });
    ro.observe(containerEl);
    measure();
    setScrollTop(containerEl.scrollTop);

    return () => {
      containerEl.removeEventListener('scroll', onScroll);
      ro.disconnect();
    };
  }, [containerEl]);

  // Build a cumulative-offset array so we can binary-search for the visible range.
  const { offsets, totalSize } = useMemo(() => {
    const off = new Float64Array(count + 1);
    for (let i = 0; i < count; i++) off[i + 1] = off[i] + estimateSize(i);
    return { offsets: off, totalSize: off[count] };
  }, [count, estimateSize]);

  // Binary search: first item whose bottom edge is >= scrollTop
  const startIndex = useMemo(() => {
    if (count === 0) return 0;
    let lo = 0, hi = count - 1;
    while (lo < hi) {
      const mid = (lo + hi) >> 1;
      if (offsets[mid + 1] < scrollTop) lo = mid + 1;
      else hi = mid;
    }
    return Math.max(0, lo - overscan);
  }, [offsets, scrollTop, count, overscan]);

  // Binary search: last item whose top edge is <= scrollTop + containerHeight
  const endIndex = useMemo(() => {
    if (count === 0) return -1;
    const bottom = scrollTop + containerHeight;
    let lo = startIndex, hi = count - 1;
    while (lo < hi) {
      const mid = (lo + hi + 1) >> 1;
      if (offsets[mid] <= bottom) lo = mid;
      else hi = mid - 1;
    }
    return Math.min(count - 1, lo + overscan);
  }, [offsets, scrollTop, containerHeight, startIndex, count, overscan]);

  const virtualItems = useMemo<VirtualItem[]>(() => {
    const items: VirtualItem[] = [];
    for (let i = startIndex; i <= endIndex; i++) {
      items.push({ index: i, key: i, start: offsets[i], size: offsets[i + 1] - offsets[i] });
    }
    return items;
  }, [startIndex, endIndex, offsets]);

  /** Programmatically scroll the container so the given item is visible. */
  const scrollToIndex = useCallback(
    (index: number, align: 'start' | 'center' | 'end' = 'start') => {
      if (!containerEl || index < 0 || index >= count) return;
      const itemStart = offsets[index];
      const itemSize = offsets[index + 1] - offsets[index];
      let top: number;
      if (align === 'center') top = itemStart - (containerHeight - itemSize) / 2;
      else if (align === 'end') top = itemStart - containerHeight + itemSize;
      else top = itemStart;
      containerEl.scrollTo({ top: Math.max(0, top), behavior: 'smooth' });
    },
    [containerEl, offsets, count, containerHeight],
  );

  return { virtualItems, totalSize, scrollToIndex };
}
