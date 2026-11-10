import { PageLayout } from '../app/PageLayout';
import styles from './DashboardPage.module.css';

/** Placeholder: the backend has no user-management endpoints yet (list, suspend, reinstate). */
export function UsersPage() {
  return (
    <PageLayout title="Users">
      <h1>Users</h1>
      <p className={styles.muted}>User management is coming next.</p>
    </PageLayout>
  );
}
