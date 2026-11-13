import { z } from 'zod';

/**
 * Client-side rules mirror the backend's request validation (SignupRequest/LoginRequest) so
 * users get feedback before a round trip. The backend still validates everything itself.
 */
const email = z
  .string()
  .trim()
  .min(1, 'Enter your email address')
  .max(254, 'Email address must be 254 characters or fewer')
  .pipe(z.email('Enter a valid email address, like name@example.com'));

export const LoginInputSchema = z.object({
  email,
  password: z.string().min(1, 'Enter your password'),
});

const newPassword = z
  .string()
  .min(8, 'Password must be at least 8 characters')
  .max(72, 'Password must be 72 characters or fewer');

export const SignupInputSchema = z.object({
  displayName: z
    .string()
    .trim()
    .min(1, 'Enter your name')
    .max(80, 'Name must be 80 characters or fewer'),
  email,
  password: newPassword,
});

export const ForgotPasswordInputSchema = z.object({
  email,
});

export const ResetPasswordInputSchema = z.object({
  newPassword,
});

export type LoginInput = z.infer<typeof LoginInputSchema>;
export type SignupInput = z.infer<typeof SignupInputSchema>;
export type ForgotPasswordInput = z.infer<typeof ForgotPasswordInputSchema>;
export type ResetPasswordInput = z.infer<typeof ResetPasswordInputSchema>;
