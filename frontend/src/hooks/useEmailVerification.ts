import { useMutation, useQueryClient } from '@tanstack/react-query';
import { queryKeys } from '../state/queryClient';
import { useServices } from './useServices';

export function useVerifyEmail() {
  const { emailVerification } = useServices();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (token: string) => emailVerification.verify(token),
    onSuccess: () => {
      // The caller might be signed in right now (same browser as the signup); refresh so the
      // route guard lets them straight through instead of waiting for the next stale refetch.
      queryClient.invalidateQueries({ queryKey: queryKeys.currentUser });
    },
  });
}

export function useResendVerificationEmail() {
  const { emailVerification } = useServices();
  return useMutation({ mutationFn: () => emailVerification.resend() });
}
