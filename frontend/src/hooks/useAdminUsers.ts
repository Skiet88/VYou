import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { AdminUserQuery } from '../schemas/admin';
import { isApiError } from '../services';
import { queryKeys } from '../state/queryClient';
import { useServices } from './useServices';

export function useAdminUsers(query: AdminUserQuery) {
  const { admin } = useServices();
  const key = queryKeys.adminUsersPage(query);
  return useQuery({
    queryKey: key,
    queryFn: () => admin.listUsers(query),
    // Keep the current page visible only while paging the same search/filter.
    placeholderData: (previous, previousQuery) =>
      previousQuery && previousQuery.queryKey[2] === key[2] && previousQuery.queryKey[3] === key[3] ? previous : undefined,
  });
}

function useUserAction(action: 'suspend' | 'reinstate') {
  const { admin } = useServices();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (userId: string) => (action === 'suspend' ? admin.suspend(userId) : admin.reinstate(userId)),
    onSettled: () => queryClient.invalidateQueries({ queryKey: queryKeys.adminUsers }),
  });
}

export const useSuspendUser = () => useUserAction('suspend');
export const useReinstateUser = () => useUserAction('reinstate');

/** The backend's 409 messages are written for people ("You can't suspend your own account"). */
export function adminActionMessage(error: unknown): string {
  if (!isApiError(error)) return 'Something went wrong. Please try again.';
  if (error.kind === 'network') return 'Could not reach VYou. Check your connection and try again.';
  if (error.status === 409 && error.detail) return error.detail;
  if (error.status === 404) return 'That user no longer exists.';
  if (error.status === 403) return 'You no longer have permission to do that.';
  return 'Something went wrong on our side. Please try again.';
}
