import { useEffect, useRef } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useDocumentTitle } from '../../hooks/useDocumentTitle';
import { useVerifyEmail } from '../../hooks/useEmailVerification';
import { AuthLayout } from '../auth/AuthLayout';
import { Button } from '../ui/Button';

/** Public: reached from the link in a verification email, signed in or not. */
export function VerifyEmailPage() {
  useDocumentTitle('Verify your email');
  const [params] = useSearchParams();
  const token = params.get('token');
  const verify = useVerifyEmail();
  const attempted = useRef(false);

  useEffect(() => {
    if (token && !attempted.current) {
      attempted.current = true;
      verify.mutate(token);
    }
  }, [token, verify]);

  const body = !token ? (
    <p>This link is missing its verification code. Check that you copied the whole link from the email.</p>
  ) : verify.isSuccess ? (
    <p>Your email is verified. You can now use VYou.</p>
  ) : verify.isError ? (
    <p>That verification link is invalid or has expired. Request a new one from inside the app.</p>
  ) : (
    <p role="status">Verifying…</p>
  );

  return (
    <AuthLayout
      title="Verify your email"
      footer={<Link to={verify.isSuccess ? '/' : '/login'}>{verify.isSuccess ? 'Continue to VYou' : 'Go to sign in'}</Link>}
    >
      {body}
      {verify.isError && (
        <Button busy={verify.isPending} busyLabel="Retrying…" onClick={() => token && verify.mutate(token)}>
          Try again
        </Button>
      )}
    </AuthLayout>
  );
}
