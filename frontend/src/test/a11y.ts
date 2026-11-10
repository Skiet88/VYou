import { axe } from 'vitest-axe';
import { expect } from 'vitest';

/**
 * Fails with a readable list of rule ids and the offending markup. Axe's colour-contrast rule is
 * off here only because jsdom cannot render colours; contrast is checked against the real design
 * tokens in src/styles/contrast.test.ts instead.
 */
export async function expectNoAxeViolations(container: Element) {
  const results = await axe(container, { rules: { 'color-contrast': { enabled: false } } });
  const summary = results.violations.map((v) => `${v.id}: ${v.nodes.map((n) => n.html).join(' | ')}`);
  expect(summary).toEqual([]);
}
