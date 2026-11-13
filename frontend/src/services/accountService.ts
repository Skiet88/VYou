import type { DeleteAccountInput } from '../schemas/account';
import type { HttpClient } from './httpClient';

export interface AccountService {
  /** Schedules the caller's own account for deletion (30-day grace period) and ends the session. */
  requestDeletion(input: DeleteAccountInput): Promise<void>;
}

export function createAccountService(http: HttpClient): AccountService {
  return {
    requestDeletion: (input) => http.postWithoutResponse('/account/delete', input),
  };
}
