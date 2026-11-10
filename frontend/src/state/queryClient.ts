import { MutationCache, QueryCache, QueryClient, type QueryClientConfig } from '@tanstack/react-query';
import { isApiError } from '../services';

export const queryKeys = {
  currentUser: ['auth', 'currentUser'] as const,
  /** Prefix for every page of the list, so one invalidation refreshes all of them. */
  myInterviews: ['interviews', 'mine'] as const,
  myInterviewsPage: (query: { page?: number; size?: number; status?: string }) =>
    ['interviews', 'mine', query.status ?? 'ALL', query.page ?? 0, query.size ?? 10] as const,
  interview: (id: string) => ['interviews', 'detail', id] as const,
  adminUsers: ['admin', 'users'] as const,
  adminUsersPage: (query: { search?: string; status?: string; page?: number; size?: number }) =>
    ['admin', 'users', query.search ?? '', query.status ?? 'ALL', query.page ?? 0, query.size ?? 20] as const,
};

const sessionEnded = (error: unknown) => isApiError(error) && error.kind === 'http' && error.status === 401;

/** `retryDelayMs` exists so tests don't have to wait out real backoff; production uses the default. */
export function createQueryClient(options: { retryDelayMs?: number } = {}): QueryClient {
  // A 401 from any request means the session is gone (expired, signed out elsewhere, or the
  // account was suspended). Treat it as signed out everywhere: the route guard then sends the user
  // to sign in (and back afterwards), and nothing cached from the old session stays in memory.
  const onError = (error: unknown) => {
    if (!sessionEnded(error)) return;
    client.removeQueries({ predicate: (query) => query.queryKey[0] !== 'auth' });
    client.setQueryData(queryKeys.currentUser, null);
  };

  const config: QueryClientConfig = {
    queryCache: new QueryCache({ onError }),
    mutationCache: new MutationCache({ onError }),
    defaultOptions: {
      queries: {
        ...(options.retryDelayMs === undefined ? {} : { retryDelay: options.retryDelayMs }),
        staleTime: 30_000,
        refetchOnWindowFocus: false,
        // Retrying a 4xx never helps (and 429 would just burn more of the rate limit).
        retry: (failureCount, error) =>
          failureCount < 2 && !(isApiError(error) && error.status >= 400 && error.status < 500),
      },
      mutations: { retry: false },
    },
  };
  const client = new QueryClient(config);
  return client;
}
