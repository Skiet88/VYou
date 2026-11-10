import { useLocation, useNavigate } from 'react-router-dom';
import { LoginInputSchema } from '../../schemas/auth';
import { authErrorMessage } from '../../hooks/authErrorMessage';
import { useLogin } from '../../hooks/useAuth';
import { useZodForm } from '../../hooks/useZodForm';
import { safeRedirect } from '../../util/safeRedirect';
import { Button } from '../ui/Button';
import { ErrorSummary, type SummaryItem } from '../ui/ErrorSummary';
import { TextField } from '../ui/TextField';
import styles from './AuthForm.module.css';

export function LoginForm() {
  const navigate = useNavigate();
  const location = useLocation();
  const login = useLogin();
  const { values, errors, submitCount, setValue, handleSubmit } = useZodForm(LoginInputSchema, { email: '', password: '' });

  const items: SummaryItem[] = [
    ...(errors.email ? [{ fieldId: 'email', message: errors.email }] : []),
    ...(errors.password ? [{ fieldId: 'password', message: errors.password }] : []),
    ...(login.isError ? [{ message: authErrorMessage(login.error) }] : []),
  ];

  const onSubmit = handleSubmit((input) => {
    login.mutate(input, {
      onSuccess: () => navigate(safeRedirect((location.state as { from?: unknown } | null)?.from), { replace: true }),
    });
  });

  return (
    <form className={styles.form} onSubmit={onSubmit} noValidate>
      <ErrorSummary items={items} focusKey={`${submitCount}:${login.isError ? 'server' : ''}`} />
      <TextField
        id="email"
        label="Email address"
        type="email"
        inputMode="email"
        autoComplete="username"
        autoCapitalize="none"
        spellCheck={false}
        value={values.email}
        onValueChange={(value) => setValue('email', value)}
        error={errors.email}
      />
      <TextField
        id="password"
        label="Password"
        type="password"
        autoComplete="current-password"
        value={values.password}
        onValueChange={(value) => setValue('password', value)}
        error={errors.password}
      />
      <Button type="submit" variant="primary" className={styles.submit} busy={login.isPending} busyLabel="Signing in…">
        Sign in
      </Button>
    </form>
  );
}
