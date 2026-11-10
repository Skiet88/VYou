import { useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import type { SessionDetails, Summary } from '../../schemas/interview';
import { interviewProblem } from '../../hooks/interviewErrorMessage';
import { useGenerateSummary } from '../../hooks/useInterviews';
import { ButtonLink } from '../ui/ButtonLink';
import { ScoreList, ScoreValue } from '../ui/ScoreList';
import { ErrorState, LoadingState } from '../ui/StatusMessage';
import { LEVEL_LABELS } from './labels';
import styles from './Interview.module.css';

/** Summary first; the question-by-question breakdown is one level down (interview detail page). */
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
      </div>
      {answered === 0 ? (
        <p>You ended this interview before answering any questions, so there is nothing to summarise.</p>
      ) : summary ? (
        <SummaryPanel summary={summary} />
      ) : (
        <SummaryLoader interviewId={session.id} />
      )}
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
