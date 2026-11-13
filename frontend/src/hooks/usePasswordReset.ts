import { useMutation } from '@tanstack/react-query';
import { useServices } from './useServices';

export function useRequestPasswordReset() {
  const { passwordReset } = useServices();
  return useMutation({ mutationFn: (email: string) => passwordReset.requestReset(email) });
}

export function useResetPassword() {
  const { passwordReset } = useServices();
  return useMutation({
    mutationFn: ({ token, newPassword }: { token: string; newPassword: string }) =>
      passwordReset.resetPassword(token, newPassword),
  });
}
