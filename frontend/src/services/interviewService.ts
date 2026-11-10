import {
  AnswerResultSchema,
  CompletionSchema,
  InterviewPageSchema,
  SessionDetailsSchema,
  SessionSchema,
  SummarySchema,
  type AnswerResult,
  type Completion,
  type InterviewListQuery,
  type InterviewPage,
  type Session,
  type SessionDetails,
  type StartInterviewInput,
  type Summary,
} from '../schemas/interview';
import type { HttpClient } from './httpClient';

export interface InterviewService {
  start(input: StartInterviewInput): Promise<Session>;
  /** One page of the caller's interviews, newest first. */
  mine(query?: InterviewListQuery): Promise<InterviewPage>;
  details(id: string): Promise<SessionDetails>;
  submitAnswer(id: string, answer: string): Promise<AnswerResult>;
  complete(id: string): Promise<Completion>;
  /** Returns the summary, generating it if it does not exist yet. */
  summary(id: string): Promise<Summary>;
}

const path = (id: string) => `/interviews/${encodeURIComponent(id)}`;

export function createInterviewService(http: HttpClient): InterviewService {
  return {
    // Voice/video are not offered yet; every interview is text.
    start: (input) => http.post('/interviews', { ...input, mode: 'TEXT' }, SessionSchema),
    mine: ({ page = 0, size = 10, status } = {}) => {
      const params = new URLSearchParams({ page: String(page), size: String(size) });
      if (status) params.set('status', status);
      return http.get(`/interviews/mine?${params.toString()}`, InterviewPageSchema);
    },
    details: (id) => http.get(path(id), SessionDetailsSchema),
    submitAnswer: (id, answer) => http.post(`${path(id)}/answers`, { answer }, AnswerResultSchema),
    complete: (id) => http.post(`${path(id)}/complete`, undefined, CompletionSchema),
    summary: (id) => http.post(`${path(id)}/summary`, undefined, SummarySchema),
  };
}
