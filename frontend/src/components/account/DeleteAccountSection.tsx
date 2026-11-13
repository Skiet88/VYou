import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { DeleteAccountInputSchema } from '../../schemas/account';
import { authErrorMessage } from '../../hooks/authErrorMessage';
import { useDeleteAccount } from '../../hooks/useAccount';
import { useZodForm } from '../../hooks/useZodForm';
import { Button } from '../ui/Button';
import { ConfirmDialog } from '../ui/ConfirmDialog';
import { ErrorSummary, type SummaryItem } from '../ui/ErrorSummary';
import { TextField } from '../ui/TextField';
import styles from './DeleteAccountSection.module.css';

/**
 * Asks for the password up front (so a shared or unlocked device can't delete the account with
 * one click), then a final confirmation dialog before actually sending the request.
 */
export function DeleteAccountSection() {
  const navigate = useNavigate();
  const deleteAccount = useDeleteAccount();
  const { values, errors, submitCount, setValue, handleSubmit } = useZodForm(DeleteAccountInputSchema, { password: '' });
  const [confirming, setConfirming] = useState<string | null>(null);

  const items: SummaryItem[] = [
    ...(errors.password ? [{ fieldId: 'password', message: errors.password }] : []),
    ...(deleteAccount.isError ? [{ message: authErrorMessage(deleteAccount.error) }] : []),
  ];

  const onSubmit = handleSubmit((input) => setConfirming(input.password));

  return (
    <section className={styles.section} aria-labelledby="delete-account-heading">
      <h2 id="delete-account-heading">Delete account</h2>
      <p className={styles.muted}>
        Your account, interviews, answers and feedback are deleted 30 days after you confirm. Signing in again
        before then cancels it and restores everything, exactly as it was.
      </p>
      <form className={styles.form} noValidate onSubmit={onSubmit}>
        <ErrorSummary items={items} focusKey={`${submitCount}:${deleteAccount.isError ? 'server' : ''}`} />
        <TextField
          id="password"
          label="Confirm your password"
          type="password"
          autoComplete="current-password"
          value={values.password}
          onValueChange={(value) => setValue('password', value)}
          error={errors.password}
        />
        <Button type="submit">Delete account</Button>
      </form>
      <ConfirmDialog
        open={confirming !== null}
        title="Delete your account?"
        confirmLabel="Delete account"
        cancelLabel="Cancel"
        busy={deleteAccount.isPending}
        busyLabel="Deleting…"
        onCancel={() => setConfirming(null)}
        onConfirm={() =>
          confirming !== null &&
          deleteAccount.mutate(
            { password: confirming },
            {
              onSuccess: () => navigate('/login', { replace: true, state: { accountDeleted: true } }),
              onError: () => setConfirming(null),
            },
          )
        }
      >
        This deletes your account in 30 days. You’ll be signed out now. Sign in again before then to cancel it.
      </ConfirmDialog>
    </section>
  );
}
