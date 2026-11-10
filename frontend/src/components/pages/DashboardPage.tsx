import { useCurrentUser } from '../../hooks/useAuth';
import { PageLayout } from '../app/PageLayout';
import { ButtonLink } from '../ui/ButtonLink';
import styles from './DashboardPage.module.css';

/** Interim dashboard: one clear action. Progress and recent results come with the dashboard work. */
export function DashboardPage() {
  const { data: user } = useCurrentUser();
  return (
    <PageLayout title="Dashboard">
      <h1>Welcome, {user?.displayName}</h1>
      <p className={styles.muted}>Practise an interview and get feedback on every answer.</p>
      <div>
        <ButtonLink to="/interviews/new" variant="primary">
          Start interview
        </ButtonLink>
      </div>
    </PageLayout>
  );
}
