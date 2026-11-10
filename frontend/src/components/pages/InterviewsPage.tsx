import { PageLayout } from '../app/PageLayout';
import { ButtonLink } from '../ui/ButtonLink';
import styles from './DashboardPage.module.css';

/** Placeholder until the interview history list is built. */
export function InterviewsPage() {
  return (
    <PageLayout title="Interviews">
      <h1>Interviews</h1>
      <p className={styles.muted}>Your past and in-progress interviews will be listed here.</p>
      <div>
        <ButtonLink to="/interviews/new" variant="primary">
          Start interview
        </ButtonLink>
      </div>
    </PageLayout>
  );
}
