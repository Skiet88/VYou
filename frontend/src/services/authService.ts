import type { LoginInput, SignupInput } from '../schemas/auth';
import { UserSchema, type User } from '../schemas/user';
import { ApiError } from './apiError';
import type { HttpClient } from './httpClient';

export interface AuthService {
  login(input: LoginInput): Promise<User>;
  signup(input: SignupInput): Promise<User>;
  logout(): Promise<void>;
  /** The signed-in user, or null when there is no valid session. */
  currentUser(): Promise<User | null>;
}

export function createAuthService(http: HttpClient): AuthService {
  return {
    login: (input) => http.post('/auth/login', input, UserSchema),
    signup: (input) => http.post('/auth/signup', input, UserSchema),
    logout: () => http.postWithoutResponse('/auth/logout'),
    async currentUser() {
      try {
        return await http.get('/auth/me', UserSchema);
      } catch (error) {
        if (error instanceof ApiError && error.kind === 'http' && error.status === 401) return null;
        throw error;
      }
    },
  };
}
