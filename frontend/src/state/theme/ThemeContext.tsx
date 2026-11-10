import { createContext, useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { appConfig } from '../../config/env';

export type Theme = 'light' | 'dark';

export interface ThemeState {
  theme: Theme;
  toggleTheme: () => void;
}

export const ThemeContext = createContext<ThemeState | null>(null);

/** localStorage can throw (privacy modes, blocked storage); the theme must still work without it. */
function readStoredTheme(): Theme | null {
  try {
    const value = window.localStorage.getItem(appConfig.themeStorageKey);
    return value === 'light' || value === 'dark' ? value : null;
  } catch {
    return null;
  }
}

function systemTheme(): Theme {
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

/**
 * The user's explicit choice is persisted; with no choice the app follows the OS setting. The
 * choice is applied as data-theme on <html>, which tokens.css keys off - no inline styles, so the
 * strict Content-Security-Policy still holds.
 */
export function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setTheme] = useState<Theme>(() => readStoredTheme() ?? systemTheme());

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
  }, [theme]);

  const toggleTheme = useCallback(() => {
    setTheme((current) => {
      const next: Theme = current === 'dark' ? 'light' : 'dark';
      try {
        window.localStorage.setItem(appConfig.themeStorageKey, next);
      } catch {
        // Still switch for this visit even if it can't be remembered.
      }
      return next;
    });
  }, []);

  const value = useMemo(() => ({ theme, toggleTheme }), [theme, toggleTheme]);
  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}
