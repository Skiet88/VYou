const FALLBACK = '/';
const AUTH_PAGES = new Set(['/login', '/signup']);

/**
 * Only follows "return to" targets that are paths inside this app. Anything that could leave
 * the origin (absolute URLs, protocol-relative "//host", backslash tricks, javascript: etc.) or
 * loop back to an auth page falls back to the dashboard. Prevents open redirects.
 */
export function safeRedirect(target: unknown): string {
  if (typeof target !== 'string' || target.length === 0 || target.length > 2048) return FALLBACK;
  if (!target.startsWith('/')) return FALLBACK;
  if (target.startsWith('//') || target.startsWith('/\\')) return FALLBACK;
  // Control characters (including tab/newline) can be stripped by browsers into "//host".
  for (let i = 0; i < target.length; i++) {
    const code = target.charCodeAt(i);
    if (code < 0x20 || code === 0x7f) return FALLBACK;
  }
  const path = target.split(/[?#]/, 1)[0];
  if (AUTH_PAGES.has(path)) return FALLBACK;
  return target;
}
