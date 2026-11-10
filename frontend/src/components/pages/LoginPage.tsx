import { Link } from 'react-router-dom';
import { useDocumentTitle } from '../../hooks/useDocumentTitle';
import { AuthLayout } from '../auth/AuthLayout';
import { LoginForm } from '../auth/LoginForm';

export function LoginPage() {
  useDocumentTitle('Sign in');
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
      <LoginForm />
    </AuthLayout>
  );
}
