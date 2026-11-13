import { z } from 'zod';
import { UserSchema } from './user';

export const LoginResultSchema = z.object({
  user: UserSchema,
  /** True when this login cancelled a scheduled account deletion. */
  accountReactivated: z.boolean(),
});

export const DeleteAccountInputSchema = z.object({
  password: z.string().min(1, 'Enter your password'),
});

export type LoginResult = z.infer<typeof LoginResultSchema>;
export type DeleteAccountInput = z.infer<typeof DeleteAccountInputSchema>;
