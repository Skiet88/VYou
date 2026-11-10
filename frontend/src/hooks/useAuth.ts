import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { LoginInput, SignupInput } from '../schemas/auth';
import type { User } from '../schemas/user';
import { queryKeys } from '../state/queryClient';
import { useServices } from './useServices';

/** Who is signed in. `data` is null when there is no session; the query cache is the only copy. */
export function useCurrentUser() {
  const { auth } = useServices();
  return useQuery<User | null>({ queryKey: queryKeys.currentUser, queryFn: () => auth.currentUser() });
}

export function useLogin() {
  const { auth } = useServices();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: LoginInput) => auth.login(input),
    onSuccess: (user) => queryClient.setQueryData(queryKeys.currentUser, user),
  });
}

export function useSignup() {
  const { auth } = useServices();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: SignupInput) => auth.signup(input),
    onSuccess: (user) => queryClient.setQueryData(queryKeys.currentUser, user),
  });
}

export function useLogout() {
  const { auth } = useServices();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => auth.logout(),
    onSettled: () => {
      // Whatever the server said, drop every cached response tied to the old session.
      queryClient.clear();
      queryClient.setQueryData(queryKeys.currentUser, null);
    },
  });
}
