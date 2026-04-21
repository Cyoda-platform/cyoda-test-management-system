import { useState, useEffect } from 'react';
import { getAuthToken } from '../lib/api';

/**
 * Fetches a protected URL with the Bearer token and returns a blob: object URL.
 * Returns { blobUrl, error } — blobUrl is null while loading; error is true if fetch failed.
 * Automatically revokes the blob URL on unmount to prevent memory leaks.
 */
export function useAuthenticatedImageUrl(url: string | null | undefined): { blobUrl: string | null; error: boolean } {
  const [blobUrl, setBlobUrl] = useState<string | null>(null);
  const [error, setError] = useState(false);

  useEffect(() => {
    if (!url) {
      setBlobUrl(null);
      setError(false);
      return;
    }

    let revoked = false;
    let objectUrl: string | null = null;
    setBlobUrl(null);
    setError(false);

    const token = getAuthToken();
    const headers: HeadersInit = token ? { Authorization: `Bearer ${token}` } : {};

    fetch(url, { credentials: 'include', headers })
      .then(r => {
        if (!r.ok) throw new Error(`Image fetch failed: ${r.status}`);
        return r.blob();
      })
      .then(blob => {
        if (revoked) return;
        objectUrl = URL.createObjectURL(blob);
        setBlobUrl(objectUrl);
      })
      .catch(() => {
        if (!revoked) setError(true);
      });

    return () => {
      revoked = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [url]);

  return { blobUrl, error };
}
