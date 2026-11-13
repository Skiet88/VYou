import { z } from 'zod';

/** The backend's RFC 7807 error body (application/problem+json). */
export const ProblemDetailSchema = z.object({
  title: z.string().nullish(),
  status: z.number().nullish(),
  detail: z.string().nullish(),
  reasons: z.array(z.string()).nullish(),
  /** A stable, machine-checkable error code (e.g. "EMAIL_NOT_VERIFIED"), when the backend sets one. */
  properties: z.object({ code: z.string().optional() }).nullish(),
});

export type ProblemDetail = z.infer<typeof ProblemDetailSchema>;
