import type { HttpClient } from './httpClient';

export interface EmailVerificationService {
  /** Consumes a token from a verification email link. Works whether or not the caller is signed in. */
  verify(token: string): Promise<void>;
  /** Resends the caller's own verification email; a no-op if already verified. */
  resend(): Promise<void>;
}

export function createEmailVerificationService(http: HttpClient): EmailVerificationService {
  return {
    verify: (token) => http.postWithoutResponse('/email-verification/verify', { token }),
    resend: () => http.postWithoutResponse('/email-verification/resend'),
  };
}
