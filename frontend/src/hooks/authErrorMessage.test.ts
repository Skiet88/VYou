import { describe, expect, it } from 'vitest';
import { ApiError } from '../services';
import { authErrorMessage } from './authErrorMessage';

describe('authErrorMessage', () => {
  it('uses the backend generic detail for a 401', () => {
    expect(authErrorMessage(new ApiError('http', 401, 'Invalid email or password'))).toBe('Invalid email or password');
  });

  it('gives fixed messages for rate limiting, CSRF expiry and network failure', () => {
    expect(authErrorMessage(new ApiError('http', 429, 'anything'))).toMatch(/too many attempts/i);
    expect(authErrorMessage(new ApiError('http', 403, null))).toMatch(/session has expired/i);
    expect(authErrorMessage(new ApiError('network', 0, null))).toMatch(/could not reach/i);
  });

  it('never shows server detail for unexpected statuses', () => {
    expect(authErrorMessage(new ApiError('http', 500, 'java.lang.NullPointerException at ...'))).not.toMatch(/java/);
  });
});
