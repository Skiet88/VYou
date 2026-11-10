import { Link } from 'react-router-dom';
import { useDocumentTitle } from '../../hooks/useDocumentTitle';
import { AuthLayout } from '../auth/AuthLayout';
import { SignupForm } from '../auth/SignupForm';

export function SignupPage() {
  useDocumentTitle('Create an account');
  return (
    <AuthLayout
      title="Create an account"
      footer={
        <>
          Already have an account? <Link to="/login">Sign in instead</Link>
        </>
      }
    >
      <SignupForm />
    </AuthLayout>
  );
}
