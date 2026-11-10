import { QueryClient, type QueryClientConfig } from '@tanstack/react-query';
import { isApiError } from '../services';

/** `retryDelayMs` exists so tests don't have to wait out real backoff; production uses the default. */
export function createQueryClient(options: { retryDelayMs?: number } = {}): QueryClient {
  const config: QueryClientConfig = {
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
  return new QueryClient(config);
}

export const queryKeys = {
  currentUser: ['auth', 'currentUser'] as const,
};
