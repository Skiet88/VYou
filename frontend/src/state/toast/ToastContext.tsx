import { createContext, useCallback, useContext, useRef, useState, type ReactNode } from 'react';
import styles from './Toast.module.css';

type ToastType = 'success' | 'error' | 'info';
interface ToastState {
  id: number;
  message: string;
  type: ToastType;
}

interface ToastContextValue {
  showToast: (message: string, type?: ToastType) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);
const DURATION_MS = 4000;

/** A single transient notice at a time, dismissed automatically; later calls replace it. */
export function ToastProvider({ children }: { children: ReactNode }) {
  const [toast, setToast] = useState<ToastState | null>(null);
  const timeoutRef = useRef<ReturnType<typeof setTimeout>>();
  const nextId = useRef(0);

  const showToast = useCallback((message: string, type: ToastType = 'info') => {
    clearTimeout(timeoutRef.current);
    const id = ++nextId.current;
    setToast({ id, message, type });
    timeoutRef.current = setTimeout(() => {
      setToast((current) => (current?.id === id ? null : current));
    }, DURATION_MS);
  }, []);

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      <div className={styles.viewport} aria-live="polite">
        {toast && (
          <div role="status" className={[styles.toast, styles[toast.type]].join(' ')}>
            {toast.message}
          </div>
        )}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast(): ToastContextValue {
  const context = useContext(ToastContext);
  if (!context) throw new Error('useToast must be used within a ToastProvider');
  return context;
}
