import { appConfig } from '../config/env';
import { readCookie } from '../util/cookies';
import { createAuthService, type AuthService } from './authService';
import { createHttpClient } from './httpClient';

export interface Services {
  auth: AuthService;
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
  return { auth: createAuthService(http) };
}

export type { AuthService } from './authService';
export { ApiError, isApiError } from './apiError';
