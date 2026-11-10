import styles from './RadioGroup.module.css';

export interface RadioOption {
  value: string;
  label: string;
  hint?: string;
}

interface RadioGroupProps {
  /** Also the id of the first radio, so an error-summary link to "#name" lands on the group. */
  name: string;
  legend: string;
  legendHidden?: boolean;
  options: RadioOption[];
  value: string;
  onValueChange: (value: string) => void;
  error?: string;
}

export function RadioGroup({ name, legend, legendHidden, options, value, onValueChange, error }: RadioGroupProps) {
  const errorId = error ? `${name}-error` : undefined;
  return (
    <fieldset className={styles.fieldset} aria-describedby={errorId} aria-invalid={error ? true : undefined}>
      <legend className={legendHidden ? 'visually-hidden' : styles.legend}>{legend}</legend>
      {error && (
        <p id={errorId} className={styles.error}>
          <span className="visually-hidden">Error: </span>
          {error}
        </p>
      )}
      <div className={styles.options}>
        {options.map((option, index) => {
          const id = index === 0 ? name : `${name}-${option.value}`;
          const hintId = option.hint ? `${id}-hint` : undefined;
          return (
            <div key={option.value} className={styles.option}>
              <input
                id={id}
                type="radio"
                name={name}
                value={option.value}
                checked={value === option.value}
                onChange={() => onValueChange(option.value)}
                aria-describedby={hintId}
                className={styles.input}
              />
              <label htmlFor={id} className={styles.label}>
                {option.label}
                {option.hint && (
                  <span id={hintId} className={styles.hint}>
                    {option.hint}
                  </span>
                )}
              </label>
            </div>
          );
        })}
      </div>
    </fieldset>
  );
}
