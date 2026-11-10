import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { AnswerResult, InterviewListQuery, SessionDetails, StartInterviewInput } from '../schemas/interview';
import { queryKeys } from '../state/queryClient';
import { useServices } from './useServices';

export function useMyInterviews(query: InterviewListQuery = {}) {
  const { interviews } = useServices();
  const key = queryKeys.myInterviewsPage(query);
  return useQuery({
    queryKey: key,
    queryFn: () => interviews.mine(query),
    // While paging within the same filter, keep showing the current page until the next one
    // arrives. Never across a filter change: old rows under a new filter label would mislead.
    placeholderData: (previous, previousQuery) =>
      previousQuery && previousQuery.queryKey[2] === key[2] && previousQuery.queryKey[4] === key[4] ? previous : undefined,
  });
}

export function useInterview(id: string) {
  const { interviews } = useServices();
  return useQuery({ queryKey: queryKeys.interview(id), queryFn: () => interviews.details(id) });
}

export function useStartInterview() {
  const { interviews } = useServices();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: StartInterviewInput) => interviews.start(input),
    onSuccess: (session) => {
      queryClient.setQueryData<SessionDetails>(queryKeys.interview(session.id), { session, turns: [], summary: null });
      void queryClient.invalidateQueries({ queryKey: queryKeys.myInterviews });
    },
  });
}

/** Applies an answer result to the cached interview straight away, so the next question shows without a refetch. */
function applyAnswer(details: SessionDetails, result: AnswerResult): SessionDetails {
  const completed = result.sessionStatus === 'COMPLETED';
  return {
    ...details,
    session: {
      ...details.session,
      status: result.sessionStatus,
      questionsAnswered: result.turn.sequenceNumber,
      currentQuestion: completed ? null : result.nextQuestion,
    },
    turns: [...details.turns.filter((t) => t.sequenceNumber !== result.turn.sequenceNumber), result.turn],
  };
}

export function useSubmitAnswer(id: string) {
  const { interviews } = useServices();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (answer: string) => interviews.submitAnswer(id, answer),
    onSuccess: (result) => {
      queryClient.setQueryData<SessionDetails>(queryKeys.interview(id), (current) => (current ? applyAnswer(current, result) : current));
      void queryClient.invalidateQueries({ queryKey: queryKeys.interview(id) });
      void queryClient.invalidateQueries({ queryKey: queryKeys.myInterviews });
    },
  });
}

export function useCompleteInterview(id: string) {
  const { interviews } = useServices();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => interviews.complete(id),
    onSettled: () => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.interview(id) });
      void queryClient.invalidateQueries({ queryKey: queryKeys.myInterviews });
    },
  });
}

export function useGenerateSummary(id: string) {
  const { interviews } = useServices();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => interviews.summary(id),
    onSuccess: (summary) => {
      queryClient.setQueryData<SessionDetails>(queryKeys.interview(id), (current) => (current ? { ...current, summary } : current));
    },
  });
}
