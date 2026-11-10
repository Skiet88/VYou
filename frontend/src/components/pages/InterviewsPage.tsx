import { Link, useSearchParams } from 'react-router-dom';
import type { InterviewStatus } from '../../schemas/interview';
import { useMyInterviews } from '../../hooks/useInterviews';
import { PageLayout } from '../app/PageLayout';
import { InterviewList } from '../interview/InterviewList';
import { ButtonLink } from '../ui/ButtonLink';
import { FilterLinks } from '../ui/FilterLinks';
import { Pagination } from '../ui/Pagination';
import { ErrorState, LoadingState } from '../ui/StatusMessage';
import styles from './InterviewsPage.module.css';

const PAGE_SIZE = 10;
const FILTERS: Array<{ label: string; status?: InterviewStatus }> = [
  { label: 'All' },
  { label: 'In progress', status: 'IN_PROGRESS' },
  { label: 'Completed', status: 'COMPLETED' },
];

/** Only allow-listed values from the address bar reach the API. */
function readQuery(params: URLSearchParams) {
  const rawStatus = params.get('status');
  const status = FILTERS.find((f) => f.status === rawStatus)?.status;
  const rawPage = Number(params.get('page') ?? '1');
  const page = Number.isInteger(rawPage) && rawPage >= 1 && rawPage <= 10_000 ? rawPage - 1 : 0;
  return { status, page };
}

function href(status: InterviewStatus | undefined, page: number) {
  const params = new URLSearchParams();
  if (status) params.set('status', status);
  if (page > 0) params.set('page', String(page + 1));
  const query = params.toString();
  return `/interviews${query ? `?${query}` : ''}`;
}

export function InterviewsPage() {
  const [params] = useSearchParams();
  const { status, page } = readQuery(params);
  const list = useMyInterviews({ status, page, size: PAGE_SIZE });
  const filterLabel = FILTERS.find((f) => f.status === status)?.label ?? 'All';

  return (
    <PageLayout title={page > 0 ? `Interviews, page ${page + 1}` : 'Interviews'}>
      <div className={styles.header}>
        <h1>Interviews</h1>
        <ButtonLink to="/interviews/new" variant="primary">
          Start interview
        </ButtonLink>
      </div>
      <FilterLinks
        label="Filter interviews"
        options={FILTERS.map((f) => ({ label: f.label, href: href(f.status, 0), current: f.status === status }))}
      />
      {list.isPending ? (
        <LoadingState />
      ) : list.isError ? (
        <ErrorState message="Your interviews could not be loaded. Check your connection and try again." onRetry={() => list.refetch()} />
      ) : list.data.items.length === 0 ? (
        <EmptyState hasAny={list.data.totalItems > 0} filtered={status !== undefined} filterLabel={filterLabel} />
      ) : (
        <>
          <p className={styles.count} aria-live="polite">
            {list.data.totalItems} {list.data.totalItems === 1 ? 'interview' : 'interviews'}
            {status ? ` · ${filterLabel.toLowerCase()}` : ''}
          </p>
          <div aria-busy={list.isPlaceholderData || undefined} className={list.isPlaceholderData ? styles.stale : undefined}>
            <InterviewList items={list.data.items} />
          </div>
          <Pagination page={list.data.page} totalPages={list.data.totalPages} hrefFor={(p) => href(status, p)} />
        </>
      )}
    </PageLayout>
  );
}

function EmptyState({ hasAny, filtered, filterLabel }: { hasAny: boolean; filtered: boolean; filterLabel: string }) {
  if (hasAny) {
    // Past the last page (e.g. an old bookmark).
    return (
      <p className={styles.muted}>
        There are no interviews on this page. <Link to="/interviews">Go to the first page</Link>.
      </p>
    );
  }
  if (filtered) {
    return <p className={styles.muted}>You have no {filterLabel.toLowerCase()} interviews.</p>;
  }
  return (
    <div className={styles.empty}>
      <h2>You haven’t done any interviews yet</h2>
      <p className={styles.muted}>Start one to practise answering questions and get feedback on every answer.</p>
    </div>
  );
}
