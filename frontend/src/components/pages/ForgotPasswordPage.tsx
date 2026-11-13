import { Link } from 'react-router-dom';
import { useDocumentTitle } from '../../hooks/useDocumentTitle';
import { ForgotPasswordForm } from '../auth/ForgotPasswordForm';
import { AuthLayout } from '../auth/AuthLayout';

export function ForgotPasswordPage() {
  useDocumentTitle('Reset your password');
  return (
    <AuthLayout
      title="Reset your password"
      intro="Enter the email address on your account and we'll send you a link to reset your password."
      footer={<Link to="/login">Back to sign in</Link>}
    >
      <ForgotPasswordForm />
    </AuthLayout>
  );
}
