import { isApiError } from '../services';

export interface InterviewProblem {
  message: string;
  /** Specific reasons from answer screening (e.g. "Answer is too short (3 words, minimum 5)"). */
  reasons: readonly string[];
}

/**
 * One user-facing explanation for any interview failure. Where the user's typed answer is still
 * on screen, the message says so, so nobody retypes an answer they think was lost.
 */
export function interviewProblem(error: unknown): InterviewProblem {
  const plain = (message: string): InterviewProblem => ({ message, reasons: [] });
  if (!isApiError(error)) return plain('Something went wrong. Please try again.');
  if (error.kind === 'network') return plain('Could not reach VYou. Check your connection, then try again. Your answer is still here.');
  if (error.kind === 'invalid-response') return plain('Something went wrong on our side. Please try again.');
  switch (error.status) {
    case 400:
      return plain(error.detail ?? 'Check your answer and try again.');
    case 403:
      return plain('Your session has expired. Refresh the page and sign in again.');
    case 404:
      return plain('We could not find this interview.');
    case 409:
      return plain('This interview has already ended, or it changed in another tab. Reload the page to see its latest state.');
    case 413:
      return plain('That answer is too long to send. Please shorten it.');
    case 422:
      return { message: 'Your answer could not be used yet.', reasons: error.reasons };
    case 429:
      return plain('You have reached the limit for now. Please wait a while before continuing. Your answer is still here.');
    case 502:
    case 503:
      return plain('The AI interviewer is unavailable right now. Your answer is still here. Try again in a moment.');
    default:
      return plain('Something went wrong on our side. Please try again.');
  }
}

export function isNotFoundError(error: unknown): boolean {
  return isApiError(error) && error.kind === 'http' && error.status === 404;
}
