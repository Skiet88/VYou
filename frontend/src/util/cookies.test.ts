import { describe, expect, it } from 'vitest';
import { readCookie } from './cookies';

describe('readCookie', () => {
  it('finds a cookie among others and decodes it', () => {
    expect(readCookie('XSRF-TOKEN', 'theme=dark; XSRF-TOKEN=abc%2D123; other=1')).toBe('abc-123');
  });

  it('does not match a cookie whose name merely contains the requested name', () => {
    expect(readCookie('XSRF-TOKEN', 'MY-XSRF-TOKEN=nope')).toBeNull();
  });

  it('returns null when absent or malformed', () => {
    expect(readCookie('XSRF-TOKEN', '')).toBeNull();
    expect(readCookie('XSRF-TOKEN', 'XSRF-TOKEN=%E0%A4%A')).toBeNull();
  });
});
