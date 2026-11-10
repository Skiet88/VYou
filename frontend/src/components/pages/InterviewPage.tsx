import { Link, useParams } from 'react-router-dom';
import { useInterview } from '../../hooks/useInterviews';
import { interviewProblem, isNotFoundError } from '../../hooks/interviewErrorMessage';
import { PageLayout } from '../app/PageLayout';
import { InterviewFlow } from '../interview/InterviewFlow';
import { ErrorState, LoadingState } from '../ui/StatusMessage';

const crumbs = (label: string) => [{ label: 'Interviews', to: '/interviews' }, { label }];

export function InterviewPage() {
  const { id = '' } = useParams();
  const interview = useInterview(id);

  if (interview.isPending) {
    return (
      <PageLayout title="Interview" breadcrumbs={crumbs('Interview')}>
        <LoadingState />
      </PageLayout>
    );
  }
  if (interview.isError) {
    const notFound = isNotFoundError(interview.error);
    return (
      <PageLayout title="Interview not found" breadcrumbs={crumbs('Interview')}>
        {notFound ? (
          <>
            <h1>Interview not found</h1>
            <p>
              It may have been removed, or the link is wrong. <Link to="/interviews">Go to your interviews</Link>.
            </p>
          </>
        ) : (
          <ErrorState message={interviewProblem(interview.error).message} onRetry={() => interview.refetch()} />
        )}
      </PageLayout>
    );
  }

  const { session } = interview.data;
  return (
    <PageLayout title={`${session.jobRole} interview`} width="narrow" breadcrumbs={crumbs(session.jobRole)}>
      <InterviewFlow details={interview.data} />
    </PageLayout>
  );
}
