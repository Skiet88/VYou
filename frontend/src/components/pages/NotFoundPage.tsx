import { Link } from 'react-router-dom';
import { useDocumentTitle } from '../../hooks/useDocumentTitle';
import styles from './DashboardPage.module.css';

export function NotFoundPage() {
  useDocumentTitle('Page not found');
  return (
    <main className={styles.main}>
      <h1>Page not found</h1>
      <p className={styles.muted}>
        Check the address, or <Link to="/">go to your dashboard</Link>.
      </p>
    </main>
  );
}
