import { useNavigate } from 'react-router-dom';
import { emailVerificationErrorMessage } from '../../hooks/emailVerificationErrorMessage';
import { useLogout } from '../../hooks/useAuth';
import { useDocumentTitle } from '../../hooks/useDocumentTitle';
import { useResendVerificationEmail } from '../../hooks/useEmailVerification';
import { AuthLayout } from '../auth/AuthLayout';
import { Button } from '../ui/Button';

/** Shown instead of the app for a signed-in but unverified account; nothing else is reachable. */
export function EmailVerificationPendingPage() {
  useDocumentTitle('Verify your email');
  const resend = useResendVerificationEmail();
  const logout = useLogout();
  const navigate = useNavigate();

  return (
    <AuthLayout
      title="Verify your email"
      intro="We sent you a link to confirm your email address. Open it to continue."
      footer={
        <Button
          busy={logout.isPending}
          busyLabel="Signing out…"
          onClick={() => logout.mutate(undefined, { onSettled: () => navigate('/login', { replace: true }) })}
        >
          Sign out
        </Button>
      }
    >
      <p role="status">
        {resend.isSuccess && 'Email sent. Check your inbox (and spam folder).'}
        {resend.isError && emailVerificationErrorMessage(resend.error)}
      </p>
      <Button busy={resend.isPending} busyLabel="Sending…" onClick={() => resend.mutate()}>
        Resend verification email
      </Button>
    </AuthLayout>
  );
}
