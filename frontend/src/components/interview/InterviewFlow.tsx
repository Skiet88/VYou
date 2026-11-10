import { useEffect, useRef, useState } from 'react';
import type { SessionDetails, Turn } from '../../schemas/interview';
import { interviewProblem } from '../../hooks/interviewErrorMessage';
import { useCompleteInterview, useSubmitAnswer } from '../../hooks/useInterviews';
import { Button } from '../ui/Button';
import { ConfirmDialog } from '../ui/ConfirmDialog';
import { AnswerForm } from './AnswerForm';
import { CompletionView } from './CompletionView';
import { FeedbackCard } from './FeedbackCard';
import styles from './Interview.module.css';

type EvaluatedTurn = Turn & { evaluation: NonNullable<Turn['evaluation']> };

/**
 * Drives one interview: question -> answer -> (practice mode) feedback -> next question, until
 * completion. Owns the "feedback card is showing" state, so the final answer's feedback is still
 * shown even though the interview is already complete on the server by then.
 */
export function InterviewFlow({ details }: { details: SessionDetails }) {
  const { session } = details;
  const submit = useSubmitAnswer(session.id);
  const complete = useCompleteInterview(session.id);
  const [pendingFeedback, setPendingFeedback] = useState<EvaluatedTurn | null>(null);
  const [savedNotice, setSavedNotice] = useState<number | null>(null);
  const [confirmEnd, setConfirmEnd] = useState(false);
  const [problemKey, setProblemKey] = useState(0);

  if (pendingFeedback) {
    return (
      <FeedbackCard
        turn={pendingFeedback}
        isLast={session.status === 'COMPLETED'}
        onContinue={() => setPendingFeedback(null)}
      />
    );
  }
  if (session.status === 'COMPLETED') return <CompletionView details={details} />;

  const questionNumber = session.questionsAnswered + 1;
  const problem = submit.isError ? interviewProblem(submit.error) : complete.isError ? interviewProblem(complete.error) : null;

  return (
    <section className={styles.section} aria-labelledby="question-heading">
      <QuestionHeader number={questionNumber} total={session.totalQuestions} />
      {savedNotice !== null && (
        <p role="status" className={styles.notice}>
          Answer {savedNotice} saved. Your scores will appear when you finish.
        </p>
      )}
      <p className={styles.question}>{session.currentQuestion}</p>
      <AnswerForm
        key={session.questionsAnswered}
        busy={submit.isPending}
        problem={problem}
        problemKey={problemKey}
        onSubmit={(answer) =>
          submit.mutate(answer, {
            onSuccess: (result) => {
              if (result.turn.evaluation && session.feedbackMode === 'INSTANT') {
                setSavedNotice(null);
                setPendingFeedback(result.turn as EvaluatedTurn);
              } else {
                setSavedNotice(result.turn.sequenceNumber);
              }
            },
            onError: () => setProblemKey((k) => k + 1),
          })
        }
        secondaryAction={
          <Button onClick={() => setConfirmEnd(true)} disabled={submit.isPending}>
            End interview
          </Button>
        }
      />
      <ConfirmDialog
        open={confirmEnd}
        title="End this interview now?"
        confirmLabel="End interview"
        cancelLabel="Keep going"
        busy={complete.isPending}
        busyLabel="Ending…"
        onCancel={() => setConfirmEnd(false)}
        onConfirm={() =>
          complete.mutate(undefined, {
            onSettled: () => setConfirmEnd(false),
            onError: () => setProblemKey((k) => k + 1),
          })
        }
      >
        {session.questionsAnswered === 0
          ? "You haven't answered any questions yet, so there won't be a summary."
          : `You've answered ${session.questionsAnswered} of ${session.totalQuestions} questions. You'll get a summary of those answers.`}
      </ConfirmDialog>
    </section>
  );
}

function QuestionHeader({ number, total }: { number: number; total: number }) {
  const headingRef = useRef<HTMLHeadingElement>(null);
  const isFirst = useRef(true);
  // Each new question is announced by moving focus to its heading (not on first page load,
  // where the app shell already handles focus).
  useEffect(() => {
    if (isFirst.current) {
      isFirst.current = false;
      return;
    }
    headingRef.current?.focus();
  }, [number]);

  return (
    <div className={styles.header}>
      <h1 id="question-heading" ref={headingRef} tabIndex={-1}>
        Question {number} of {total}
      </h1>
      <progress className={styles.progress} value={number - 1} max={total} aria-label={`${number - 1} of ${total} questions answered`} />
    </div>
  );
}
