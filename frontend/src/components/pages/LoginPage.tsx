import { Link, useLocation } from 'react-router-dom';
import { useDocumentTitle } from '../../hooks/useDocumentTitle';
import { DismissibleBanner } from '../app/DismissibleBanner';
import { AuthLayout } from '../auth/AuthLayout';
import { LoginForm } from '../auth/LoginForm';

export function LoginPage() {
  useDocumentTitle('Sign in');
  const location = useLocation();
  const accountDeleted = (location.state as { accountDeleted?: boolean } | null)?.accountDeleted;
  return (
    <AuthLayout
      title="Sign in"
      intro="Practise interviews and get feedback on every answer."
      footer={
        <>
          New to VYou? <Link to="/signup">Create an account</Link>
        </>
      }
    >
      {accountDeleted && (
        <DismissibleBanner>
          Your account will be deleted in 30 days. Sign in again before then to cancel it.
        </DismissibleBanner>
      )}
      <LoginForm />
    </AuthLayout>
  );
}
