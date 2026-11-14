import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

/** Reads the actual tokens.css, so this test fails if anyone edits a colour below AA. */
const css = readFileSync(resolve(__dirname, 'tokens.css'), 'utf8');

function tokens(blockSelector: string): Record<string, string> {
  const start = css.indexOf(`${blockSelector} {`);
  if (start === -1) throw new Error(`No ${blockSelector} block in tokens.css`);
  const block = css.slice(start, css.indexOf('}', start));
  return Object.fromEntries([...block.matchAll(/--(color-[\w-]+):\s*(#[0-9a-fA-F]{6})/g)].map((m) => [m[1], m[2]]));
}

function luminance(hex: string): number {
  const [r, g, b] = [1, 3, 5].map((i) => {
    const c = parseInt(hex.slice(i, i + 2), 16) / 255;
    return c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
  });
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

function contrast(a: string, b: string): number {
  const [hi, lo] = [luminance(a), luminance(b)].sort((x, y) => y - x);
  return (hi + 0.05) / (lo + 0.05);
}

// [foreground, background, minimum]: 4.5:1 for text, 3:1 for UI component boundaries/focus.
const PAIRS: Array<[string, string, number]> = [
  ['color-text', 'color-bg', 4.5],
  ['color-text', 'color-surface', 4.5],
  ['color-text-muted', 'color-bg', 4.5],
  ['color-text-muted', 'color-surface', 4.5],
  ['color-accent', 'color-bg', 4.5], // links
  ['color-on-accent', 'color-accent', 4.5], // primary button text
  ['color-on-accent', 'color-accent-hover', 4.5],
  ['color-error', 'color-bg', 4.5],
  ['color-error', 'color-error-surface', 4.5],
  ['color-text', 'color-error-surface', 4.5],
  ['color-border', 'color-bg', 3], // input boundaries
  ['color-border', 'color-surface', 3],
  ['color-focus', 'color-bg', 3], // focus ring
  ['color-text', 'color-card', 4.5],
  ['color-text-muted', 'color-card', 4.5],
  ['color-accent', 'color-card', 4.5],
  ['color-accent', 'color-accent-surface', 4.5], // active pill / badge
  ['color-error', 'color-card', 4.5],
  ['color-success', 'color-success-surface', 4.5],
  ['color-warning', 'color-warning-surface', 4.5],
  ['color-info', 'color-info-surface', 4.5],
  ['color-border', 'color-card', 3],
];

describe.each([
  ['light theme', ':root'],
  ['dark theme', ":root[data-theme='dark']"],
])('%s meets WCAG 2.1 AA contrast', (_name, selector) => {
  const theme = tokens(selector);

  it.each(PAIRS)('%s on %s', (fg, bg, minimum) => {
    expect(theme[fg], `missing --${fg}`).toBeDefined();
    expect(theme[bg], `missing --${bg}`).toBeDefined();
    expect(contrast(theme[fg], theme[bg])).toBeGreaterThanOrEqual(minimum);
  });
});

it('the OS-preference dark block matches the explicit dark theme exactly', () => {
  const explicit = tokens(":root[data-theme='dark']");
  const start = css.indexOf(':root:not([data-theme]) {');
  const block = css.slice(start, css.indexOf('}', start));
  const viaMedia = Object.fromEntries([...block.matchAll(/--(color-[\w-]+):\s*(#[0-9a-fA-F]{6})/g)].map((m) => [m[1], m[2]]));
  expect(viaMedia).toEqual(explicit);
});
