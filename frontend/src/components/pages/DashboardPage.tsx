import { useCurrentUser } from '../../hooks/useAuth';
import { useMyInterviews } from '../../hooks/useInterviews';
import { PageLayout } from '../app/PageLayout';
import { ProgressSummary } from '../dashboard/ProgressSummary';
import { RecentResults } from '../dashboard/RecentResults';
import { ResumeCard } from '../dashboard/ResumeCard';
import { ButtonLink } from '../ui/ButtonLink';
import { ErrorState, LoadingState } from '../ui/StatusMessage';
import styles from '../dashboard/Dashboard.module.css';

const RECENT_FOR_AVERAGE = 5;

/**
 * Only what's needed to decide what to do next: continue (or start) an interview, a glance at
 * progress, and the latest results. Everything else is one click away in Interviews.
 */
export function DashboardPage() {
  const { data: user } = useCurrentUser();
  const inProgress = useMyInterviews({ status: 'IN_PROGRESS', size: 1 });
  const completed = useMyInterviews({ status: 'COMPLETED', size: RECENT_FOR_AVERAGE });

  const loading = inProgress.isPending || completed.isPending;
  const failed = inProgress.isError || completed.isError;
  const unfinished = inProgress.data?.items[0];
  const hasCompleted = (completed.data?.totalItems ?? 0) > 0;

  return (
    <PageLayout title="Dashboard">
      <div className={styles.stack}>
        <h1>Welcome, {user?.displayName}</h1>
        {loading ? (
          <LoadingState />
        ) : failed ? (
          <ErrorState
            message="Your dashboard could not be loaded. Check your connection and try again."
            onRetry={() => {
              void inProgress.refetch();
              void completed.refetch();
            }}
          />
        ) : (
          <>
            {unfinished ? (
              <ResumeCard interview={unfinished} />
            ) : (
              <section className={styles.card} aria-labelledby="start-heading">
                <h2 id="start-heading">{hasCompleted ? 'Ready for another?' : 'Your first interview'}</h2>
                <p className={styles.muted}>
                  {hasCompleted
                    ? 'Practise again to build on your last results.'
                    : 'Pick a role and level, then answer a short set of questions, about 15 minutes. You’ll get feedback on every answer.'}
                </p>
                <div className={styles.actions}>
                  <ButtonLink to="/interviews/new" variant="primary">
                    Start interview
                  </ButtonLink>
                </div>
              </section>
            )}
            {hasCompleted && completed.data && (
              <>
                <ProgressSummary completed={completed.data} />
                <RecentResults items={completed.data.items} />
              </>
            )}
          </>
        )}
      </div>
    </PageLayout>
  );
}
