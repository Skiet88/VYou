import { useEffect, useRef, type ReactNode } from 'react';
import { Button } from './Button';
import styles from './ConfirmDialog.module.css';

interface ConfirmDialogProps {
  open: boolean;
  title: string;
  children: ReactNode;
  confirmLabel: string;
  cancelLabel: string;
  busy?: boolean;
  busyLabel?: string;
  onConfirm: () => void;
  onCancel: () => void;
}

/**
 * Native <dialog> shown with showModal(): the browser provides the focus trap, Escape to close,
 * an inert background and correct dialog semantics. Focus starts on the safe choice (cancel).
 */
export function ConfirmDialog({ open, title, children, confirmLabel, cancelLabel, busy, busyLabel, onConfirm, onCancel }: ConfirmDialogProps) {
  const ref = useRef<HTMLDialogElement>(null);
  const cancelRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.open) {
      dialog.showModal();
      cancelRef.current?.focus();
    }
    if (!open && dialog.open) dialog.close();
  }, [open]);

  return (
    <dialog
      ref={ref}
      className={styles.dialog}
      aria-labelledby="confirm-dialog-title"
      onCancel={(event) => {
        event.preventDefault();
        if (!busy) onCancel();
      }}
    >
      <h2 id="confirm-dialog-title">{title}</h2>
      <div className={styles.body}>{children}</div>
      <div className={styles.actions}>
        <Button ref={cancelRef} onClick={onCancel} disabled={busy}>
          {cancelLabel}
        </Button>
        <Button variant="primary" onClick={onConfirm} busy={busy} busyLabel={busyLabel}>
          {confirmLabel}
        </Button>
      </div>
    </dialog>
  );
}
