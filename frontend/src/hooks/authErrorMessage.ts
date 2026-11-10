import { isApiError } from '../services';

/**
 * Turns any auth failure into one user-facing sentence. The backend's own `detail` is used where
 * it is already deliberately generic (it never says which of email/password was wrong, or
 * whether an email is registered); everything else gets a fixed message, so no internal detail
 * leaks into the UI.
 */
export function authErrorMessage(error: unknown): string {
  if (!isApiError(error)) return 'Something went wrong. Please try again.';
  if (error.kind === 'network') return 'Could not reach VYou. Check your connection and try again.';
  if (error.kind === 'invalid-response') return 'Something went wrong on our side. Please try again.';
  switch (error.status) {
    case 400:
      return error.detail ?? 'Check your details and try again.';
    case 401:
      return error.detail ?? 'Invalid email or password';
    case 403:
      return 'Your session has expired. Refresh the page and try again.';
    case 409:
      return error.detail ?? 'Could not create an account with the details provided';
    case 413:
      return 'That was too long to send. Please shorten it and try again.';
    case 429:
      return 'Too many attempts. Please wait a minute and try again.';
    default:
      return 'Something went wrong on our side. Please try again.';
  }
}
