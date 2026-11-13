import type { HttpClient } from './httpClient';

export interface PasswordResetService {
  /** Always resolves, whether or not the email is registered - the backend never reveals that. */
  requestReset(email: string): Promise<void>;
  /** Consumes a token from a reset email link. Works whether or not the caller is signed in. */
  resetPassword(token: string, newPassword: string): Promise<void>;
}

export function createPasswordResetService(http: HttpClient): PasswordResetService {
  return {
    requestReset: (email) => http.postWithoutResponse('/password-reset/request', { email }),
    resetPassword: (token, newPassword) => http.postWithoutResponse('/password-reset/reset', { token, newPassword }),
  };
}
