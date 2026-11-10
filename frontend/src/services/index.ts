import { appConfig } from '../config/env';
import { readCookie } from '../util/cookies';
import { createAuthService, type AuthService } from './authService';
import { createHttpClient } from './httpClient';
import { createInterviewService, type InterviewService } from './interviewService';

export interface Services {
  auth: AuthService;
  interviews: InterviewService;
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
  return { auth: createAuthService(http), interviews: createInterviewService(http) };
}

export type { AuthService } from './authService';
export type { InterviewService } from './interviewService';
export { ApiError, isApiError } from './apiError';
