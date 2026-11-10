import { Link } from 'react-router-dom';
import styles from './Pagination.module.css';

interface PaginationProps {
  /** Zero-based current page. */
  page: number;
  totalPages: number;
  /** Builds the link for a zero-based page. */
  hrefFor: (page: number) => string;
}

/** Previous/next links (real links, so pages are bookmarkable and Back works). */
export function Pagination({ page, totalPages, hrefFor }: PaginationProps) {
  if (totalPages <= 1) return null;
  return (
    <nav aria-label="Pagination" className={styles.nav}>
      {page > 0 ? (
        <Link to={hrefFor(page - 1)} rel="prev" aria-label="Previous page">
          Previous
        </Link>
      ) : (
        <span aria-hidden="true" className={styles.placeholder} />
      )}
      <span className={styles.position}>
        Page {page + 1} of {totalPages}
      </span>
      {page < totalPages - 1 ? (
        <Link to={hrefFor(page + 1)} rel="next" aria-label="Next page">
          Next
        </Link>
      ) : (
        <span aria-hidden="true" className={styles.placeholder} />
      )}
    </nav>
  );
}
