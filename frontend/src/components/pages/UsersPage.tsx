import { useState, type FormEvent } from 'react';
import { useSearchParams } from 'react-router-dom';
import type { AdminUser, UserStatus } from '../../schemas/admin';
import { useCurrentUser } from '../../hooks/useAuth';
import { adminActionMessage, useAdminUsers, useReinstateUser, useSuspendUser } from '../../hooks/useAdminUsers';
import { PageLayout } from '../app/PageLayout';
import { UserRow } from '../admin/UserRow';
import { Button } from '../ui/Button';
import { ConfirmDialog } from '../ui/ConfirmDialog';
import { FilterLinks } from '../ui/FilterLinks';
import { Pagination } from '../ui/Pagination';
import { ErrorState, LoadingState } from '../ui/StatusMessage';
import { useToast } from '../../hooks/useToast';
import { TextField } from '../ui/TextField';
import styles from '../admin/UserAdmin.module.css';

const PAGE_SIZE = 20;
const MAX_SEARCH = 100;
const FILTERS: Array<{ label: string; status?: UserStatus }> = [
  { label: 'All' },
  { label: 'Active', status: 'ACTIVE' },
  { label: 'Suspended', status: 'SUSPENDED' },
];

/** Only allow-listed, bounded values from the address bar reach the API. */
function readQuery(params: URLSearchParams) {
  const status = FILTERS.find((f) => f.status === params.get('status'))?.status;
  const search = (params.get('search') ?? '').trim().slice(0, MAX_SEARCH);
  const rawPage = Number(params.get('page') ?? '1');
  const page = Number.isInteger(rawPage) && rawPage >= 1 && rawPage <= 10_000 ? rawPage - 1 : 0;
  return { status, search, page };
}

function href(search: string, status: UserStatus | undefined, page: number) {
  const params = new URLSearchParams();
  if (search) params.set('search', search);
  if (status) params.set('status', status);
  if (page > 0) params.set('page', String(page + 1));
  const query = params.toString();
  return `/users${query ? `?${query}` : ''}`;
}

/**
 * Admin-only. Shows accounts and aggregate numbers, never interview content. Suspending asks for
 * confirmation (it signs the person out immediately); reinstating does not.
 */
export function UsersPage() {
  const [params, setParams] = useSearchParams();
  const { status, search, page } = readQuery(params);
  const { data: me } = useCurrentUser();
  const users = useAdminUsers({ search: search || undefined, status, page, size: PAGE_SIZE });
  const suspend = useSuspendUser();
  const reinstate = useReinstateUser();
  const [draft, setDraft] = useState(search);
  const [confirming, setConfirming] = useState<AdminUser | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const { showToast } = useToast();

  const onSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const next = new URLSearchParams();
    const trimmed = draft.trim().slice(0, MAX_SEARCH);
    if (trimmed) next.set('search', trimmed);
    if (status) next.set('status', status);
    setParams(next);
  };

  const runAction = (mutation: typeof suspend, user: AdminUser, verb: string) => {
    setActionError(null);
    mutation.mutate(user.id, {
      onSuccess: () => showToast(`${user.displayName} ${verb}.`, 'success'),
      onError: (error) => setActionError(adminActionMessage(error)),
      onSettled: () => setConfirming(null),
    });
  };

  return (
    <PageLayout title="Users">
      <h1>Users</h1>
      <form className={styles.search} role="search" onSubmit={onSearch}>
        <div className={styles.searchField}>
          <TextField
            id="user-search"
            label="Search by name or email"
            type="search"
            maxLength={MAX_SEARCH}
            value={draft}
            onValueChange={setDraft}
          />
        </div>
        <Button type="submit">Search</Button>
      </form>
      <FilterLinks
        label="Filter users"
        options={FILTERS.map((f) => ({ label: f.label, href: href(search, f.status, 0), current: f.status === status }))}
      />
      {actionError && (
        <p role="alert" className={styles.muted}>
          {actionError}
        </p>
      )}
      {users.isPending ? (
        <LoadingState />
      ) : users.isError ? (
        <ErrorState message={adminActionMessage(users.error)} onRetry={() => users.refetch()} />
      ) : users.data.items.length === 0 ? (
        <p className={styles.muted}>{search || status ? 'No users match.' : 'There are no users yet.'}</p>
      ) : (
        <>
          <p className={styles.count}>
            {users.data.totalItems} {users.data.totalItems === 1 ? 'user' : 'users'}
            {search ? ` matching “${search}”` : ''}
          </p>
          <ul className={styles.list} aria-label="Users" aria-busy={users.isPlaceholderData || undefined}>
            {users.data.items.map((user) => (
              <UserRow
                key={user.id}
                user={user}
                isSelf={user.id === me?.id}
                busy={(suspend.isPending || reinstate.isPending) && (suspend.variables === user.id || reinstate.variables === user.id)}
                onSuspend={setConfirming}
                onReinstate={(u) => runAction(reinstate, u, 'reinstated')}
              />
            ))}
          </ul>
          <Pagination page={users.data.page} totalPages={users.data.totalPages} hrefFor={(p) => href(search, status, p)} />
        </>
      )}
      <ConfirmDialog
        open={confirming !== null}
        title={confirming ? `Suspend ${confirming.displayName}?` : 'Suspend user?'}
        confirmLabel="Suspend"
        cancelLabel="Cancel"
        busy={suspend.isPending}
        busyLabel="Suspending…"
        onCancel={() => setConfirming(null)}
        onConfirm={() => confirming && runAction(suspend, confirming, 'suspended')}
      >
        They’ll be signed out straight away and won’t be able to sign in until you reinstate them. Their interviews are kept.
      </ConfirmDialog>
    </PageLayout>
  );
}
