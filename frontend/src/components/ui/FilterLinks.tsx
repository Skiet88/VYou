import { Link } from 'react-router-dom';
import styles from './FilterLinks.module.css';

export interface FilterOption {
  label: string;
  href: string;
  current: boolean;
}

/** A small set of filters as links (bookmarkable), with the active one marked for screen readers. */
export function FilterLinks({ label, options }: { label: string; options: FilterOption[] }) {
  return (
    <nav aria-label={label}>
      <ul className={styles.list}>
        {options.map((option) => (
          <li key={option.label}>
            <Link
              to={option.href}
              aria-current={option.current ? 'true' : undefined}
              className={[styles.link, option.current ? styles.current : ''].join(' ')}
            >
              {option.label}
            </Link>
          </li>
        ))}
      </ul>
    </nav>
  );
}
