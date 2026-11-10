import { useCurrentUser } from '../../hooks/useAuth';
import { useDocumentTitle } from '../../hooks/useDocumentTitle';
import { AppHeader } from '../app/AppHeader';
import styles from './DashboardPage.module.css';

/** Placeholder until the real dashboard is built; proves the signed-in flow end to end. */
export function DashboardPage() {
  useDocumentTitle('Dashboard');
  const { data: user } = useCurrentUser();

  return (
    <>
      <AppHeader />
      <main className={styles.main}>
        <h1>Welcome, {user?.displayName}</h1>
        <p className={styles.muted}>Your dashboard is coming next.</p>
      </main>
    </>
  );
}
