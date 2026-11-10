/**
 * Environment-based config. The API is always reached through a same-origin path (the Vite dev
 * proxy, or the production reverse proxy), never a hardcoded host, so no internal URL is ever
 * bundled into the frontend.
 */
export const appConfig = {
  apiBasePath: import.meta.env.VITE_API_BASE_PATH ?? '/api',
  csrfCookieName: 'XSRF-TOKEN',
  csrfHeaderName: 'X-XSRF-TOKEN',
  themeStorageKey: 'vyou.theme',
} as const;
