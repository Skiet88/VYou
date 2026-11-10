import { describe, expect, it } from 'vitest';
import { safeRedirect } from './safeRedirect';

describe('safeRedirect', () => {
  it.each(['/', '/interviews', '/interviews/123?tab=summary#q2'])('allows in-app path %s', (path) => {
    expect(safeRedirect(path)).toBe(path);
  });

  it.each([
    ['protocol-relative', '//evil.example.com'],
    ['backslash trick', '/\\evil.example.com'],
    ['absolute URL', 'https://evil.example.com/'],
    ['javascript URL', 'javascript:alert(1)'],
    ['tab smuggling', '/\t/evil.example.com'],
    ['newline smuggling', '/\n/evil.example.com'],
    ['relative path', 'interviews'],
    ['back to sign-in', '/login'],
    ['back to sign-up', '/signup?x=1'],
    ['empty string', ''],
    ['not a string', { pathname: '/' }],
    ['undefined', undefined],
  ])('falls back to the dashboard for %s', (_label, target) => {
    expect(safeRedirect(target)).toBe('/');
  });
});
