import { Link, useSearchParams } from 'react-router-dom';
import { useDocumentTitle } from '../../hooks/useDocumentTitle';
import { AuthLayout } from '../auth/AuthLayout';
import { ResetPasswordForm } from '../auth/ResetPasswordForm';

/** Public: reached from the link in a password-reset email, signed in or not. */
export function ResetPasswordPage() {
  useDocumentTitle('Reset your password');
  const [params] = useSearchParams();
  const token = params.get('token');

  return (
    <AuthLayout title="Reset your password" footer={<Link to="/forgot-password">Request a new link</Link>}>
      {token ? (
        <ResetPasswordForm token={token} />
      ) : (
        <p>This link is missing its reset code. Check that you copied the whole link from the email.</p>
      )}
    </AuthLayout>
  );
}
