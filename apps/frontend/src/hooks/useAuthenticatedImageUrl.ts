import { useState, useEffect } from 'react';
import { getAuthToken } from '../lib/api';

/**
 * Fetches a protected URL with the Bearer token and returns a blob: object URL.
 * Returns null while loading or on error.
 * Automatically revokes the blob URL on unmount to prevent memory leaks.
 */
export function useAuthenticatedImageUrl(url: string | null | undefined): string | null {
  const [blobUrl, setBlobUrl] = useState<string | null>(null);

  useEffect(() => {
    if (!url) {
      setBlobUrl(null);
      return;
    }

    let revoked = false;
    let objectUrl: string | null = null;

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
        if (!revoked) setBlobUrl(null);
      });

    return () => {
      revoked = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [url]);

  return blobUrl;
}
