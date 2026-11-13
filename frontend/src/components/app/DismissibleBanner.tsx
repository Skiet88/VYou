import { useState, type ReactNode } from 'react';
import styles from './DismissibleBanner.module.css';

/** A one-shot notice (e.g. "deletion cancelled") that the viewer can dismiss; it never reappears. */
export function DismissibleBanner({ children }: { children: ReactNode }) {
  const [dismissed, setDismissed] = useState(false);
  if (dismissed) return null;
  return (
    <div className={styles.banner} role="status">
      <p>{children}</p>
      <button type="button" className={styles.dismiss} onClick={() => setDismissed(true)} aria-label="Dismiss this message">
        Dismiss
      </button>
    </div>
  );
}
