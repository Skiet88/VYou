import { appConfig } from '../config/env';
import { readCookie } from '../util/cookies';
import { createAccountService, type AccountService } from './accountService';
import { createAdminService, type AdminService } from './adminService';
import { createAuthService, type AuthService } from './authService';
import { createEmailVerificationService, type EmailVerificationService } from './emailVerificationService';
import { createHttpClient } from './httpClient';
import { createInterviewService, type InterviewService } from './interviewService';
import { createPasswordResetService, type PasswordResetService } from './passwordResetService';

export interface Services {
  auth: AuthService;
  interviews: InterviewService;
  admin: AdminService;
  account: AccountService;
  emailVerification: EmailVerificationService;
  passwordReset: PasswordResetService;
}

/** Production wiring. Tests build their own Services (or use these against a mocked network). */
export function createServices(): Services {
  const http = createHttpClient({
    basePath: appConfig.apiBasePath,
    csrfCookieName: appConfig.csrfCookieName,
    csrfHeaderName: appConfig.csrfHeaderName,
    csrfPrimePath: '/auth/me',
    readCookie: (name) => readCookie(name),
  });
  return {
    auth: createAuthService(http),
    interviews: createInterviewService(http),
    admin: createAdminService(http),
    account: createAccountService(http),
    emailVerification: createEmailVerificationService(http),
    passwordReset: createPasswordResetService(http),
  };
}

export type { AuthService } from './authService';
export type { InterviewService } from './interviewService';
export type { AdminService } from './adminService';
export type { AccountService } from './accountService';
export type { EmailVerificationService } from './emailVerificationService';
export type { PasswordResetService } from './passwordResetService';
export { ApiError, isApiError } from './apiError';
