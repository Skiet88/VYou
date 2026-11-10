import { useEffect, useRef } from 'react';
import styles from './ErrorSummary.module.css';

export interface SummaryItem {
  /** id of the field the message is about; omitted for form-level (e.g. server) errors. */
  fieldId?: string;
  message: string;
}

interface ErrorSummaryProps {
  items: SummaryItem[];
  /**
   * The summary takes focus whenever this changes while it has items: pass something that
   * changes on each submit attempt and when a server error arrives, but not on every keystroke,
   * so fixing a field never steals focus mid-typing.
   */
  focusKey: string | number;
}

/**
 * Shown above a form after a failed submit. It receives focus (tabIndex -1) so keyboard and
 * screen-reader users land on the problem, and each item links to its field.
 */
export function ErrorSummary({ items, focusKey }: ErrorSummaryProps) {
  const ref = useRef<HTMLDivElement>(null);
  const hasItems = items.length > 0;

  useEffect(() => {
    if (hasItems) ref.current?.focus();
    // Deliberately keyed on focusKey only: see the prop's doc comment.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [focusKey]);

  if (!hasItems) return null;
  return (
    <div ref={ref} className={styles.summary} tabIndex={-1} role="alert" aria-labelledby="error-summary-title">
      <h2 id="error-summary-title" className={styles.title}>
        There is a problem
      </h2>
      <ul className={styles.list}>
        {items.map((item) => (
          <li key={`${item.fieldId ?? 'form'}:${item.message}`}>
            {item.fieldId ? <a href={`#${item.fieldId}`}>{item.message}</a> : item.message}
          </li>
        ))}
      </ul>
    </div>
  );
}
