import { renderHook, act } from '@testing-library/react';
import { beforeEach, describe, it, expect } from 'vitest';
import useLocalStorage from './useLocalStorage';

describe('useLocalStorage', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('returns default value when key is absent', () => {
    const { result } = renderHook(() => useLocalStorage('k', 42));
    expect(result.current[0]).toBe(42);
  });

  it('reads existing value from localStorage on mount', () => {
    localStorage.setItem('k', JSON.stringify(99));
    const { result } = renderHook(() => useLocalStorage('k', 42));
    expect(result.current[0]).toBe(99);
  });

  it('setter updates state', () => {
    const { result } = renderHook(() => useLocalStorage('k', 0));
    act(() => result.current[1](7));
    expect(result.current[0]).toBe(7);
  });

  it('setter persists to localStorage', () => {
    const { result } = renderHook(() => useLocalStorage('k', 0));
    act(() => result.current[1](7));
    expect(JSON.parse(localStorage.getItem('k')!)).toBe(7);
  });

  it('setter supports functional update form', () => {
    localStorage.setItem('k', JSON.stringify({ a: 1, b: 2 }));
    const { result } = renderHook(() =>
      useLocalStorage<{ a: number; b: number }>('k', { a: 0, b: 0 })
    );
    act(() => result.current[1](prev => ({ ...prev, a: 99 })));
    expect(result.current[0]).toEqual({ a: 99, b: 2 });
    expect(JSON.parse(localStorage.getItem('k')!)).toEqual({ a: 99, b: 2 });
  });

  it('falls back to default on invalid JSON', () => {
    localStorage.setItem('k', 'not{valid');
    const { result } = renderHook(() => useLocalStorage('k', 'fallback'));
    expect(result.current[0]).toBe('fallback');
  });
});
