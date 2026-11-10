import { useNavigate } from 'react-router-dom';
import { SignupInputSchema } from '../../schemas/auth';
import { authErrorMessage } from '../../hooks/authErrorMessage';
import { useSignup } from '../../hooks/useAuth';
import { useZodForm } from '../../hooks/useZodForm';
import { Button } from '../ui/Button';
import { ErrorSummary, type SummaryItem } from '../ui/ErrorSummary';
import { TextField } from '../ui/TextField';
import styles from './AuthForm.module.css';

const FIELDS = ['displayName', 'email', 'password'] as const;

export function SignupForm() {
  const navigate = useNavigate();
  const signup = useSignup();
  const { values, errors, submitCount, setValue, handleSubmit } = useZodForm(SignupInputSchema, { displayName: '', email: '', password: '' });

  const items: SummaryItem[] = [
    ...FIELDS.flatMap((field) => (errors[field] ? [{ fieldId: field, message: errors[field]! }] : [])),
    ...(signup.isError ? [{ message: authErrorMessage(signup.error) }] : []),
  ];

  const onSubmit = handleSubmit((input) => {
    signup.mutate(input, {
      onSuccess: () => navigate('/', { replace: true }),
    });
  });

  return (
    <form className={styles.form} onSubmit={onSubmit} noValidate>
      <ErrorSummary items={items} focusKey={`${submitCount}:${signup.isError ? 'server' : ''}`} />
      <TextField
        id="displayName"
        label="Your name"
        autoComplete="name"
        value={values.displayName}
        onValueChange={(value) => setValue('displayName', value)}
        error={errors.displayName}
      />
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
      <TextField
        id="password"
        label="Create a password"
        type="password"
        autoComplete="new-password"
        hint="At least 8 characters."
        value={values.password}
        onValueChange={(value) => setValue('password', value)}
        error={errors.password}
      />
      <Button type="submit" variant="primary" className={styles.submit} busy={signup.isPending} busyLabel="Creating account…">
        Create account
      </Button>
    </form>
  );
}
