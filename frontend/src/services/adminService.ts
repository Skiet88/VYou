import { AdminUserPageSchema, AdminUserSchema, type AdminUser, type AdminUserPage, type AdminUserQuery } from '../schemas/admin';
import type { HttpClient } from './httpClient';

export interface AdminService {
  listUsers(query?: AdminUserQuery): Promise<AdminUserPage>;
  suspend(userId: string): Promise<AdminUser>;
  reinstate(userId: string): Promise<AdminUser>;
}

const userPath = (id: string) => `/admin/users/${encodeURIComponent(id)}`;

export function createAdminService(http: HttpClient): AdminService {
  return {
    listUsers: ({ search, status, page = 0, size = 20 } = {}) => {
      const params = new URLSearchParams({ page: String(page), size: String(size) });
      if (search) params.set('search', search);
      if (status) params.set('status', status);
      return http.get(`/admin/users?${params.toString()}`, AdminUserPageSchema);
    },
    suspend: (id) => http.post(`${userPath(id)}/suspend`, undefined, AdminUserSchema),
    reinstate: (id) => http.post(`${userPath(id)}/reinstate`, undefined, AdminUserSchema),
  };
}
