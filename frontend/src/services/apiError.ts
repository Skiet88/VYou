export type ApiErrorKind = 'http' | 'network' | 'invalid-response';

/**
 * One error type for everything the API layer can fail with, so hooks and components never deal
 * with raw fetch/Response objects. `detail` is the backend's own generic, user-safe message.
 */
export class ApiError extends Error {
  readonly kind: ApiErrorKind;
  readonly status: number;
  readonly detail: string | null;
  readonly reasons: readonly string[];
  readonly code: string | null;

  constructor(
    kind: ApiErrorKind,
    status: number,
    detail: string | null,
    reasons: readonly string[] = [],
    code: string | null = null,
  ) {
    super(detail ?? `Request failed (${kind}${status ? `, HTTP ${status}` : ''})`);
    this.name = 'ApiError';
    this.kind = kind;
    this.status = status;
    this.detail = detail;
    this.reasons = reasons;
    this.code = code;
  }
}

export function isApiError(error: unknown): error is ApiError {
  return error instanceof ApiError;
}
