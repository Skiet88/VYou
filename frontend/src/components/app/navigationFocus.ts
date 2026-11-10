import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';

/**
 * The router location key this page load started with. A reload restores the previous key from
 * history, so "key === 'default'" is not a reliable "fresh load" test; comparing against the key
 * the app first rendered with is. Module-level on purpose: it must survive the shell unmounting
 * and remounting (e.g. sign-in page -> dashboard) within one page load.
 */
let initialLocationKey: string | null = null;

/**
 * Call once, high in the tree. Records the first location key seen, in an effect (not during
 * render). Child effects run before parent ones, so on the very first render the shell still sees
 * "nothing recorded yet", which isInitialLocation treats as the initial location.
 */
export function useRecordInitialLocation() {
  const { key } = useLocation();
  useEffect(() => {
    if (initialLocationKey === null) initialLocationKey = key;
  }, [key]);
}

/** True only for the location the page was loaded on (no client-side navigation has happened). */
export function isInitialLocation(key: string): boolean {
  return initialLocationKey === null || key === initialLocationKey;
}
