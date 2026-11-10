import { z } from 'zod';

/** The backend's RFC 7807 error body (application/problem+json). */
export const ProblemDetailSchema = z.object({
  title: z.string().nullish(),
  status: z.number().nullish(),
  detail: z.string().nullish(),
  reasons: z.array(z.string()).nullish(),
});

export type ProblemDetail = z.infer<typeof ProblemDetailSchema>;
