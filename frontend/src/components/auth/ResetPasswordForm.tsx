import { useNavigate } from 'react-router-dom';
import { ResetPasswordInputSchema } from '../../schemas/auth';
import { authErrorMessage } from '../../hooks/authErrorMessage';
import { useResetPassword } from '../../hooks/usePasswordReset';
import { useZodForm } from '../../hooks/useZodForm';
import { Button } from '../ui/Button';
import { ErrorSummary, type SummaryItem } from '../ui/ErrorSummary';
import { TextField } from '../ui/TextField';
import styles from './AuthForm.module.css';

export function ResetPasswordForm({ token }: { token: string }) {
  const navigate = useNavigate();
  const resetPassword = useResetPassword();
  const { values, errors, submitCount, setValue, handleSubmit } = useZodForm(ResetPasswordInputSchema, { newPassword: '' });

  const items: SummaryItem[] = [
    ...(errors.newPassword ? [{ fieldId: 'newPassword', message: errors.newPassword }] : []),
    ...(resetPassword.isError ? [{ message: authErrorMessage(resetPassword.error) }] : []),
  ];

  const onSubmit = handleSubmit((input) => {
    resetPassword.mutate(
      { token, newPassword: input.newPassword },
      { onSuccess: () => navigate('/login', { replace: true, state: { passwordReset: true } }) },
    );
  });

  return (
    <form className={styles.form} onSubmit={onSubmit} noValidate>
      <ErrorSummary items={items} focusKey={`${submitCount}:${resetPassword.isError ? 'server' : ''}`} />
      <TextField
        id="newPassword"
        label="New password"
        type="password"
        autoComplete="new-password"
        hint="At least 8 characters."
        value={values.newPassword}
        onValueChange={(value) => setValue('newPassword', value)}
        error={errors.newPassword}
      />
      <Button type="submit" variant="primary" className={styles.submit} busy={resetPassword.isPending} busyLabel="Resetting…">
        Reset password
      </Button>
    </form>
  );
}
