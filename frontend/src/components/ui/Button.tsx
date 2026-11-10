import type { ButtonHTMLAttributes } from 'react';
import styles from './Button.module.css';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  /** One primary action per screen; everything else is secondary. */
  variant?: 'primary' | 'secondary';
  busy?: boolean;
  busyLabel?: string;
}

export function Button({ variant = 'secondary', busy = false, busyLabel, children, disabled, type = 'button', className, ...rest }: ButtonProps) {
  return (
    <button
      {...rest}
      type={type}
      className={[styles.button, styles[variant], className].filter(Boolean).join(' ')}
      disabled={disabled || busy}
      aria-busy={busy || undefined}
    >
      {busy && busyLabel ? busyLabel : children}
    </button>
  );
}
