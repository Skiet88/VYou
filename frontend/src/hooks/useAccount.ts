import { useMutation, useQueryClient } from '@tanstack/react-query';
import type { DeleteAccountInput } from '../schemas/account';
import { queryKeys } from '../state/queryClient';
import { useServices } from './useServices';

export function useDeleteAccount() {
  const { account } = useServices();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: DeleteAccountInput) => account.requestDeletion(input),
    // See the comment on skipSessionEndedHandling in state/queryClient.ts: a wrong password here
    // is a 401 on an otherwise-live session, not a dead one.
    meta: { skipSessionEndedHandling: true },
    onSuccess: () => {
      // The server already ended the session; drop everything cached under the old one.
      queryClient.clear();
      queryClient.setQueryData(queryKeys.currentUser, null);
    },
  });
}
