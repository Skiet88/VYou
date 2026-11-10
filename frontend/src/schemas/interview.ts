import { z } from 'zod';

/** Mirrors the backend's interview DTOs. Every response is validated against these. */
export const ExperienceLevelSchema = z.enum(['INTERN', 'JUNIOR', 'MID', 'SENIOR']);
export const FeedbackModeSchema = z.enum(['INSTANT', 'END_OF_INTERVIEW']);
export const InterviewStatusSchema = z.enum(['IN_PROGRESS', 'COMPLETED']);

export const SessionSchema = z.object({
  id: z.string().uuid(),
  jobRole: z.string(),
  level: ExperienceLevelSchema,
  mode: z.enum(['TEXT', 'VOICE', 'VIDEO']),
  status: InterviewStatusSchema,
  feedbackMode: FeedbackModeSchema,
  totalQuestions: z.number().int(),
  questionsAnswered: z.number().int(),
  currentQuestion: z.string().nullable(),
  startedAt: z.string(),
  endedAt: z.string().nullable(),
});

/** A row in the interview list (no question, answer or feedback text). */
export const InterviewListItemSchema = z.object({
  id: z.string().uuid(),
  jobRole: z.string(),
  level: ExperienceLevelSchema,
  status: InterviewStatusSchema,
  feedbackMode: FeedbackModeSchema,
  totalQuestions: z.number().int(),
  questionsAnswered: z.number().int(),
  averageOverall: z.number().nullable(),
  startedAt: z.string(),
  endedAt: z.string().nullable(),
});

export const pageOf = <T extends z.ZodType>(item: T) =>
  z.object({
    items: z.array(item),
    page: z.number().int(),
    size: z.number().int(),
    totalItems: z.number().int(),
    totalPages: z.number().int(),
  });

export const InterviewPageSchema = pageOf(InterviewListItemSchema);

export const EvaluationSchema = z.object({
  clarity: z.number(),
  technical: z.number(),
  confidence: z.number(),
  overall: z.number(),
  strengths: z.array(z.string()),
  improvements: z.array(z.string()),
});

export const TurnSchema = z.object({
  sequenceNumber: z.number().int(),
  question: z.string(),
  answer: z.string(),
  /** null while feedback is withheld (END_OF_INTERVIEW mode, interview still in progress). */
  evaluation: EvaluationSchema.nullable(),
  answeredAt: z.string(),
});

export const SummarySchema = z.object({
  questionsAnswered: z.number().int(),
  averageClarity: z.number(),
  averageTechnical: z.number(),
  averageConfidence: z.number(),
  averageOverall: z.number(),
  feedback: z.string(),
  topStrengths: z.array(z.string()),
  focusAreas: z.array(z.string()),
  createdAt: z.string(),
});

export const AnswerResultSchema = z.object({
  turn: TurnSchema,
  nextQuestion: z.string().nullable(),
  sessionStatus: InterviewStatusSchema,
});

export const CompletionSchema = z.object({ session: SessionSchema, summary: SummarySchema.nullable() });
export const SessionDetailsSchema = z.object({
  session: SessionSchema,
  turns: z.array(TurnSchema),
  summary: SummarySchema.nullable(),
});

/** Client-side mirrors of StartInterviewRequest / SubmitAnswerRequest validation. */
export const RoleStepSchema = z.object({
  jobRole: z
    .string()
    .trim()
    .min(1, 'Enter the role you are practising for')
    .max(120, 'Role must be 120 characters or fewer'),
});
export const LevelStepSchema = z.object({
  level: z.string().refine((value) => ExperienceLevelSchema.safeParse(value).success, 'Choose your experience level'),
});
export const FeedbackStepSchema = z.object({ feedbackMode: FeedbackModeSchema });
export const AnswerInputSchema = z.object({
  answer: z.string().trim().min(1, 'Write your answer before submitting').max(10000, 'Answer must be 10,000 characters or fewer'),
});

export type ExperienceLevel = z.infer<typeof ExperienceLevelSchema>;
export type FeedbackMode = z.infer<typeof FeedbackModeSchema>;
export type Session = z.infer<typeof SessionSchema>;
export type InterviewStatus = z.infer<typeof InterviewStatusSchema>;
export type InterviewListItem = z.infer<typeof InterviewListItemSchema>;
export type InterviewPage = z.infer<typeof InterviewPageSchema>;
export interface InterviewListQuery {
  page?: number;
  size?: number;
  status?: InterviewStatus;
}
export type Evaluation = z.infer<typeof EvaluationSchema>;
export type Turn = z.infer<typeof TurnSchema>;
export type Summary = z.infer<typeof SummarySchema>;
export type AnswerResult = z.infer<typeof AnswerResultSchema>;
export type Completion = z.infer<typeof CompletionSchema>;
export type SessionDetails = z.infer<typeof SessionDetailsSchema>;
export interface StartInterviewInput {
  jobRole: string;
  level: ExperienceLevel;
  feedbackMode: FeedbackMode;
}
