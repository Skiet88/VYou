import { useNavigate } from 'react-router-dom';
import { useLogout } from '../../hooks/useAuth';
import { Button } from '../ui/Button';
import { ThemeToggle } from '../ui/ThemeToggle';
import styles from './AppHeader.module.css';

export function AppHeader() {
  const navigate = useNavigate();
  const logout = useLogout();

  return (
    <header className={styles.header}>
      <span className={styles.brand}>VYou</span>
      <div className={styles.actions}>
        <ThemeToggle />
        <Button
          busy={logout.isPending}
          busyLabel="Signing out…"
          onClick={() => logout.mutate(undefined, { onSettled: () => navigate('/login', { replace: true }) })}
        >
          Sign out
        </Button>
      </div>
    </header>
  );
}
