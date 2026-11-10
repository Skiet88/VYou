import type { TextareaHTMLAttributes } from 'react';
import styles from './TextField.module.css';

interface TextAreaProps extends Omit<TextareaHTMLAttributes<HTMLTextAreaElement>, 'onChange' | 'value' | 'id'> {
  id: string;
  label: string;
  value: string;
  onValueChange: (value: string) => void;
  hint?: string;
  error?: string;
}

export function TextArea({ id, label, value, onValueChange, hint, error, ...rest }: TextAreaProps) {
  const hintId = hint ? `${id}-hint` : undefined;
  const errorId = error ? `${id}-error` : undefined;
  return (
    <div className={[styles.field, error ? styles.hasError : ''].filter(Boolean).join(' ')}>
      <label className={styles.label} htmlFor={id}>
        {label}
      </label>
      {hint && (
        <p id={hintId} className={styles.hint}>
          {hint}
        </p>
      )}
      {error && (
        <p id={errorId} className={styles.error}>
          <span className="visually-hidden">Error: </span>
          {error}
        </p>
      )}
      <textarea
        {...rest}
        id={id}
        name={id}
        className={[styles.input, styles.textarea].join(' ')}
        value={value}
        onChange={(event) => onValueChange(event.target.value)}
        aria-invalid={error ? true : undefined}
        aria-describedby={[hintId, errorId].filter(Boolean).join(' ') || undefined}
      />
    </div>
  );
}
