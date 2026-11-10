import type { AdminUser } from '../../schemas/admin';
import { formatDate } from '../../util/format';
import { Button } from '../ui/Button';
import styles from './UserAdmin.module.css';

interface UserRowProps {
  user: AdminUser;
  isSelf: boolean;
  busy: boolean;
  onSuspend: (user: AdminUser) => void;
  onReinstate: (user: AdminUser) => void;
}

function interviewsText(user: AdminUser) {
  if (user.interviewsTotal === 0) return 'No interviews yet';
  const base = `${user.interviewsTotal} ${user.interviewsTotal === 1 ? 'interview' : 'interviews'}, ${user.interviewsCompleted} completed`;
  return user.averageOverall === null ? base : `${base} · average ${user.averageOverall.toFixed(1)} out of 10`;
}

/** Status and role are written out in words, never conveyed by colour alone. */
export function UserRow({ user, isSelf, busy, onSuspend, onReinstate }: UserRowProps) {
  const suspended = user.status === 'SUSPENDED';
  return (
    <li className={styles.row}>
      <div className={styles.main}>
        <p className={styles.name}>
          {user.displayName}
          {isSelf && <span className={styles.tag}>You</span>}
          {user.role === 'ADMIN' && <span className={styles.tag}>Admin</span>}
          {suspended && <span className={[styles.tag, styles.suspended].join(' ')}>Suspended</span>}
        </p>
        <p className={styles.meta}>{user.email}</p>
        <p className={styles.meta}>
          Joined {formatDate(user.createdAt)} · {interviewsText(user)}
        </p>
      </div>
      <div className={styles.action}>
        {isSelf || user.role === 'ADMIN' ? null : suspended ? (
          <Button onClick={() => onReinstate(user)} busy={busy} busyLabel="Reinstating…" aria-label={`Reinstate ${user.displayName}`}>
            Reinstate
          </Button>
        ) : (
          <Button onClick={() => onSuspend(user)} disabled={busy} aria-label={`Suspend ${user.displayName}`}>
            Suspend
          </Button>
        )}
      </div>
    </li>
  );
}
