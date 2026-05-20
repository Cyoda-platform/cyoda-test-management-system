import { beforeEach, afterEach, describe, it, expect } from 'vitest';
import { setAuthToken, getAuthToken } from '@/lib/api';

/**
 * IM-12: setAuthToken must only write to localStorage in DEV mode.
 * In production, the httpOnly cookie is the auth transport — localStorage
 * is XSS-readable and must not hold the token.
 */
describe('setAuthToken localStorage gating', () => {
  const originalDEV = import.meta.env.DEV;

  beforeEach(() => {
    localStorage.clear();
    setAuthToken(null); // reset in-memory storedToken
  });

  afterEach(() => {
    (import.meta.env as Record<string, unknown>).DEV = originalDEV;
    localStorage.clear();
    setAuthToken(null);
  });

  it('persists token to localStorage in dev mode', () => {
    (import.meta.env as Record<string, unknown>).DEV = true;

    setAuthToken('my-dev-token');

    expect(localStorage.getItem('auth_token')).toBe('my-dev-token');
  });

  it('does not persist token to localStorage in production mode', () => {
    (import.meta.env as Record<string, unknown>).DEV = false;

    setAuthToken('my-prod-token');

    expect(localStorage.getItem('auth_token')).toBeNull();
  });

  it('always clears localStorage on logout regardless of mode', () => {
    localStorage.setItem('auth_token', 'leftover-from-dev');
    (import.meta.env as Record<string, unknown>).DEV = false;

    setAuthToken(null);

    expect(localStorage.getItem('auth_token')).toBeNull();
  });

  it('getAuthToken reads from localStorage only in dev mode', () => {
    localStorage.setItem('auth_token', 'stored-token');
    (import.meta.env as Record<string, unknown>).DEV = false;

    const token = getAuthToken();

    expect(token).toBeNull();
  });

  it('getAuthToken reads from localStorage in dev mode', () => {
    localStorage.setItem('auth_token', 'stored-token');
    (import.meta.env as Record<string, unknown>).DEV = true;

    const token = getAuthToken();

    expect(token).toBe('stored-token');
  });
});
