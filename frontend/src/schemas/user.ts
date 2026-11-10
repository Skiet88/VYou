import { z } from 'zod';

/** Mirrors the backend's UserResponse. Responses are validated, not trusted blindly. */
export const UserSchema = z.object({
  id: z.string().uuid(),
  email: z.string(),
  displayName: z.string(),
  role: z.enum(['USER', 'ADMIN']),
  createdAt: z.string(),
});

export type User = z.infer<typeof UserSchema>;
