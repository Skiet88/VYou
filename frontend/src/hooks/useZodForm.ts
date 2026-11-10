import { useCallback, useReducer, type FormEvent } from 'react';
import type { z } from 'zod';

export type FieldErrors<T> = Partial<Record<keyof T & string, string>>;

type ValidationResult<S extends z.ZodObject> =
  | { ok: true; data: z.output<S> }
  | { ok: false; errors: FieldErrors<z.input<S>> };

function validate<S extends z.ZodObject>(schema: S, candidate: z.input<S>): ValidationResult<S> {
  const result = schema.safeParse(candidate);
  if (result.success) return { ok: true, data: result.data };
  const errors: Record<string, string> = {};
  for (const issue of result.error.issues) {
    const field = issue.path[0];
    if (typeof field === 'string' && !(field in errors)) errors[field] = issue.message;
  }
  return { ok: false, errors: errors as FieldErrors<z.input<S>> };
}

interface FormState<V> {
  values: V;
  errors: FieldErrors<V>;
  /** Increments on every submit attempt; drives focus management in the error summary. */
  submitCount: number;
}

type FormAction<V> =
  | { type: 'change'; field: keyof V & string; value: string; revalidate: (values: V) => FieldErrors<V> | null }
  | { type: 'submitted'; errors: FieldErrors<V> };

function reducer<V>(state: FormState<V>, action: FormAction<V>): FormState<V> {
  if (action.type === 'submitted') return { ...state, errors: action.errors, submitCount: state.submitCount + 1 };
  const values = { ...state.values, [action.field]: action.value };
  if (!(action.field in state.errors)) return { ...state, values };
  // A field that is already showing an error is re-checked on every change, so the error clears
  // as soon as the value becomes valid. Untouched fields are not nagged while typing.
  const fresh = action.revalidate(values);
  const errors = { ...state.errors };
  const message = fresh?.[action.field];
  if (message) errors[action.field] = message;
  else delete errors[action.field];
  return { ...state, values, errors };
}

/**
 * Minimal form state for small forms: values and per-field errors from the same Zod schema the
 * API layer uses. Values and errors live in one reducer so they always change together.
 * `submitCount` lets the error summary take focus after each submit attempt.
 */
export function useZodForm<S extends z.ZodObject>(schema: S, initialValues: z.input<S>) {
  type Values = z.input<S>;
  const [state, dispatch] = useReducer(reducer<Values>, { values: initialValues, errors: {}, submitCount: 0 });

  const setValue = useCallback(
    (field: keyof Values & string, value: string) =>
      dispatch({
        type: 'change',
        field,
        value,
        revalidate: (values) => {
          const result = validate(schema, values);
          return result.ok ? null : result.errors;
        },
      }),
    [schema],
  );

  const handleSubmit = useCallback(
    (onValid: (data: z.output<S>) => void) => (event: FormEvent<HTMLFormElement>) => {
      event.preventDefault();
      const result = validate(schema, state.values);
      dispatch({ type: 'submitted', errors: result.ok ? {} : result.errors });
      if (result.ok) onValid(result.data);
    },
    [schema, state.values],
  );

  return { values: state.values, errors: state.errors, submitCount: state.submitCount, setValue, handleSubmit };
}
