import { useEffect, useId, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCurrentUser, useLogout } from '../../hooks/useAuth';
import { Button } from '../ui/Button';
import { ThemeToggle } from '../ui/ThemeToggle';
import styles from './AccountMenu.module.css';

/**
 * A disclosure (button + panel), not an ARIA "menu": it holds two ordinary buttons, and the
 * disclosure pattern is what screen readers and keyboards handle most predictably here.
 * Escape and clicking outside both close it; Escape returns focus to the toggle.
 */
export function AccountMenu() {
  const { data: user } = useCurrentUser();
  const logout = useLogout();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const panelId = useId();
  const containerRef = useRef<HTMLDivElement>(null);
  const toggleRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (!open) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setOpen(false);
        toggleRef.current?.focus();
      }
    };
    const onPointerDown = (event: PointerEvent) => {
      if (!containerRef.current?.contains(event.target as Node)) setOpen(false);
    };
    document.addEventListener('keydown', onKeyDown);
    document.addEventListener('pointerdown', onPointerDown);
    return () => {
      document.removeEventListener('keydown', onKeyDown);
      document.removeEventListener('pointerdown', onPointerDown);
    };
  }, [open]);

  if (!user) return null;

  return (
    <div ref={containerRef} className={styles.container}>
      <button
        ref={toggleRef}
        type="button"
        className={styles.toggle}
        aria-expanded={open}
        aria-controls={panelId}
        aria-label={`Account menu for ${user.displayName}`}
        onClick={() => setOpen((value) => !value)}
      >
        {user.displayName}
      </button>
      <div id={panelId} className={styles.panel} hidden={!open}>
        <p className={styles.email}>{user.email}</p>
        <ThemeToggle />
        <Button
          busy={logout.isPending}
          busyLabel="Signing out…"
          onClick={() => logout.mutate(undefined, { onSettled: () => navigate('/login', { replace: true }) })}
        >
          Sign out
        </Button>
      </div>
    </div>
  );
}
