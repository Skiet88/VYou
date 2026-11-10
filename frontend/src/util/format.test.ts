import { describe, expect, it } from 'vitest';
import { formatDate } from './format';

describe('formatDate', () => {
  it('formats a valid timestamp and tolerates bad input', () => {
    expect(formatDate('2026-03-05T10:00:00Z')).toMatch(/2026/);
    expect(formatDate('not a date')).toBe('');
    expect(formatDate(null)).toBe('');
  });
});
