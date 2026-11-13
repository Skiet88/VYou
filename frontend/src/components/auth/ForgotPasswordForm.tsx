import { ForgotPasswordInputSchema } from '../../schemas/auth';
import { authErrorMessage } from '../../hooks/authErrorMessage';
import { useRequestPasswordReset } from '../../hooks/usePasswordReset';
import { useZodForm } from '../../hooks/useZodForm';
import { Button } from '../ui/Button';
import { ErrorSummary, type SummaryItem } from '../ui/ErrorSummary';
import { TextField } from '../ui/TextField';
import styles from './AuthForm.module.css';

export function ForgotPasswordForm() {
  const requestReset = useRequestPasswordReset();
  const { values, errors, submitCount, setValue, handleSubmit } = useZodForm(ForgotPasswordInputSchema, { email: '' });

  const items: SummaryItem[] = [
    ...(errors.email ? [{ fieldId: 'email', message: errors.email }] : []),
    // A failed send (network, rate limit, our own error) is shown; whether the email is actually
    // registered never is - that's the whole point of this flow.
    ...(requestReset.isError ? [{ message: authErrorMessage(requestReset.error) }] : []),
  ];

  const onSubmit = handleSubmit((input) => requestReset.mutate(input.email));

  if (requestReset.isSuccess) {
    return <p role="status">If that email is registered, we&rsquo;ve sent a link to reset your password.</p>;
  }

  return (
    <form className={styles.form} onSubmit={onSubmit} noValidate>
      <ErrorSummary items={items} focusKey={`${submitCount}:${requestReset.isError ? 'server' : ''}`} />
      <TextField
        id="email"
        label="Email address"
        type="email"
        inputMode="email"
        autoComplete="email"
        autoCapitalize="none"
        spellCheck={false}
        value={values.email}
        onValueChange={(value) => setValue('email', value)}
        error={errors.email}
      />
      <Button type="submit" variant="primary" className={styles.submit} busy={requestReset.isPending} busyLabel="Sending…">
        Send reset link
      </Button>
    </form>
  );
}
