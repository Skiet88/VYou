import { isApiError } from '../services';

/** One user-facing message for a failed resend or verify attempt. */
export function emailVerificationErrorMessage(error: unknown): string {
  if (!isApiError(error)) return 'Something went wrong. Please try again.';
  if (error.kind === 'network') return 'Could not reach VYou. Check your connection and try again.';
  if (error.status === 429) return 'Too many attempts. Please wait a minute and try again.';
  return 'Could not send the email. Please try again.';
}
