import styles from './Icon.module.css';

/** Stroke paths on a 24×24 grid. Bundled locally: the strict CSP allows no icon CDN. */
const PATHS = {
  dashboard: ['M4 4h6v8H4z', 'M4 16h6v4H4z', 'M14 12h6v8h-6z', 'M14 4h6v4h-6z'],
  interviews: ['M9 5a3 3 0 0 1 6 0v6a3 3 0 0 1-6 0z', 'M5 11a7 7 0 0 0 14 0', 'M12 18v3', 'M8 21h8'],
  users: [
    'M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z',
    'M3 21v-2a4 4 0 0 1 4-4h4a4 4 0 0 1 4 4v2',
    'M16 3.13a4 4 0 0 1 0 7.75',
    'M21 21v-2a4 4 0 0 0-3-3.85',
  ],
  plus: ['M12 5v14', 'M5 12h14'],
  check: ['M5 12l5 5L20 7'],
  chart: ['M4 20h16', 'M7 20v-6', 'M12 20V8', 'M17 20v-9'],
  play: ['M7 4v16l13-8z'],
  brain: [
    'M15.5 13a3.5 3.5 0 0 0-3.5 3.5v1a3.5 3.5 0 0 0 7 0v-1.8',
    'M8.5 13a3.5 3.5 0 0 1 3.5 3.5v1a3.5 3.5 0 0 1-7 0v-1.8',
    'M17.5 16a3.5 3.5 0 0 0 0-7H17',
    'M19 9.3V6.5a3.5 3.5 0 0 0-7 0',
    'M6.5 16a3.5 3.5 0 0 1 0-7H7',
    'M5 9.3V6.5a3.5 3.5 0 0 1 7 0v10',
  ],
  user: ['M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z', 'M6 21v-2a4 4 0 0 1 4-4h4a4 4 0 0 1 4 4v2'],
  menu: ['M4 6h16', 'M4 12h16', 'M4 18h16'],
} as const;

export type IconName = keyof typeof PATHS;

/** Always decorative: every icon sits next to text that says the same thing. */
export function Icon({ name, size = 'md' }: { name: IconName; size?: 'sm' | 'md' | 'lg' }) {
  return (
    <svg
      className={[styles.icon, styles[size]].join(' ')}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      {PATHS[name].map((d) => (
        <path key={d} d={d} />
      ))}
    </svg>
  );
}
