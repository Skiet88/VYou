import { z } from 'zod';
import { pageOf } from './interview';

export const UserStatusSchema = z.enum(['ACTIVE', 'SUSPENDED']);

/** What an admin sees about a user: account details and aggregate numbers, never interview content. */
export const AdminUserSchema = z.object({
  id: z.string().uuid(),
  email: z.string(),
  displayName: z.string(),
  role: z.enum(['USER', 'ADMIN']),
  status: UserStatusSchema,
  createdAt: z.string(),
  interviewsTotal: z.number().int(),
  interviewsCompleted: z.number().int(),
  averageOverall: z.number().nullable(),
});

export const AdminUserPageSchema = pageOf(AdminUserSchema);

export type UserStatus = z.infer<typeof UserStatusSchema>;
export type AdminUser = z.infer<typeof AdminUserSchema>;
export type AdminUserPage = z.infer<typeof AdminUserPageSchema>;
export interface AdminUserQuery {
  search?: string;
  status?: UserStatus;
  page?: number;
  size?: number;
}
