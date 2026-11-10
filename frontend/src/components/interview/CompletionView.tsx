import { useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import type { SessionDetails, Summary, Turn } from '../../schemas/interview';
import { interviewProblem } from '../../hooks/interviewErrorMessage';
import { useGenerateSummary } from '../../hooks/useInterviews';
import { formatDate } from '../../util/format';
import { ButtonLink } from '../ui/ButtonLink';
import { ScoreList, ScoreValue } from '../ui/ScoreList';
import { ErrorState, LoadingState } from '../ui/StatusMessage';
import { LEVEL_LABELS } from './labels';
import styles from './Interview.module.css';

/**
 * The interview's detail view once finished: summary first, then each question collapsed, so the
 * deeper breakdown is one deliberate step down rather than all on screen at once.
 */
export function CompletionView({ details }: { details: SessionDetails }) {
  const { session, summary } = details;
  const answered = details.turns.length;

  return (
    <section className={styles.section} aria-labelledby="completion-heading">
      <div className={styles.header}>
        <h1 id="completion-heading">Interview complete</h1>
        <p className={styles.muted}>
          {session.jobRole} · {LEVEL_LABELS[session.level]} · {answered} of {session.totalQuestions} questions answered
        </p>
        <p className={styles.muted}>Taken {formatDate(session.startedAt, true)}</p>
      </div>
      {answered === 0 ? (
        <p>You ended this interview before answering any questions, so there is nothing to summarise.</p>
      ) : summary ? (
        <SummaryPanel summary={summary} />
      ) : (
        <SummaryLoader interviewId={session.id} />
      )}
      {answered > 0 && <QuestionBreakdown turns={details.turns} />}
      <div className={styles.actions}>
        <ButtonLink to="/interviews/new" variant="primary">
          Start another interview
        </ButtonLink>
        <Link to="/interviews">Back to interviews</Link>
      </div>
    </section>
  );
}

/** Interviews that finish on their last answer have no summary yet; ask for it once, with a retry. */
function SummaryLoader({ interviewId }: { interviewId: string }) {
  const generate = useGenerateSummary(interviewId);
  const requested = useRef(false);
  useEffect(() => {
    if (requested.current) return;
    requested.current = true;
    generate.mutate();
  }, [generate]);

  if (generate.isError) {
    return <ErrorState message={`Your summary could not be prepared. ${interviewProblem(generate.error).message}`} onRetry={() => generate.mutate()} />;
  }
  return <LoadingState label="Preparing your summary…" />;
}

function SummaryPanel({ summary }: { summary: Summary }) {
  return (
    <>
      <p className={styles.overall}>
        <span>
          <ScoreValue value={summary.averageOverall} decimals={1} />
        </span>
        <span className={styles.overallLabel}>average overall</span>
      </p>
      <ScoreList
        decimals={1}
        items={[
          { label: 'Clarity', value: summary.averageClarity },
          { label: 'Technical depth', value: summary.averageTechnical },
          { label: 'Confidence', value: summary.averageConfidence },
        ]}
      />
      <div>
        <h2>Your coach’s summary</h2>
        <p className={styles.feedbackText}>{summary.feedback}</p>
      </div>
      <div className={styles.columns}>
        <div>
          <h2>Strengths you showed</h2>
          <ul className={styles.list}>
            {summary.topStrengths.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </div>
        <div>
          <h2>What to practise next</h2>
          <ul className={styles.list}>
            {summary.focusAreas.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </div>
      </div>
    </>
  );
}

function QuestionBreakdown({ turns }: { turns: Turn[] }) {
  const ordered = [...turns].sort((a, b) => a.sequenceNumber - b.sequenceNumber);
  return (
    <section className={styles.breakdown} aria-labelledby="breakdown-heading">
      <h2 id="breakdown-heading">Question by question</h2>
      {ordered.map((turn) => (
        <details key={turn.sequenceNumber} className={styles.turn}>
          <summary className={styles.turnSummary}>
            <span>Question {turn.sequenceNumber}</span>
            {turn.evaluation && (
              <span className={styles.turnScore}>
                <ScoreValue value={turn.evaluation.overall} />
              </span>
            )}
          </summary>
          <div className={styles.turnBody}>
            <div>
              <h3 className={styles.subheading}>Question</h3>
              <p className={styles.feedbackText}>{turn.question}</p>
            </div>
            <div>
              <h3 className={styles.subheading}>Your answer</h3>
              <p className={styles.answerText}>{turn.answer}</p>
            </div>
            {turn.evaluation ? (
              <>
                <ScoreList
                  items={[
                    { label: 'Clarity', value: turn.evaluation.clarity },
                    { label: 'Technical depth', value: turn.evaluation.technical },
                    { label: 'Confidence', value: turn.evaluation.confidence },
                  ]}
                />
                <div className={styles.columns}>
                  <div>
                    <h3 className={styles.subheading}>What went well</h3>
                    <ul className={styles.list}>
                      {turn.evaluation.strengths.map((item) => (
                        <li key={item}>{item}</li>
                      ))}
                    </ul>
                  </div>
                  <div>
                    <h3 className={styles.subheading}>What to improve</h3>
                    <ul className={styles.list}>
                      {turn.evaluation.improvements.map((item) => (
                        <li key={item}>{item}</li>
                      ))}
                    </ul>
                  </div>
                </div>
              </>
            ) : (
              <p className={styles.muted}>Scores for this answer are not available.</p>
            )}
          </div>
        </details>
      ))}
    </section>
  );
}
