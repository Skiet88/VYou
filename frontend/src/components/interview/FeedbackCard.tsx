import { useEffect, useRef } from 'react';
import type { Turn } from '../../schemas/interview';
import { Button } from '../ui/Button';
import { ScoreList, ScoreValue } from '../ui/ScoreList';
import styles from './Interview.module.css';

interface FeedbackCardProps {
  turn: Turn & { evaluation: NonNullable<Turn['evaluation']> };
  isLast: boolean;
  onContinue: () => void;
}

/** Practice-mode feedback for one answer. AI text is rendered as plain text, never as HTML. */
export function FeedbackCard({ turn, isLast, onContinue }: FeedbackCardProps) {
  const headingRef = useRef<HTMLHeadingElement>(null);
  useEffect(() => headingRef.current?.focus(), []);
  const { evaluation } = turn;

  return (
    <section className={styles.section} aria-labelledby="feedback-heading">
      <div className={styles.header}>
        <h1 id="feedback-heading" ref={headingRef} tabIndex={-1}>
          Feedback on question {turn.sequenceNumber}
        </h1>
        <p className={styles.muted}>{turn.question}</p>
      </div>
      <p className={styles.overall}>
        <span>
          <ScoreValue value={evaluation.overall} />
        </span>
        <span className={styles.overallLabel}>overall</span>
      </p>
      <ScoreList
        items={[
          { label: 'Clarity', value: evaluation.clarity },
          { label: 'Technical depth', value: evaluation.technical },
          { label: 'Confidence', value: evaluation.confidence },
        ]}
      />
      <div className={styles.columns}>
        <div>
          <h2>What went well</h2>
          <ul className={styles.list}>
            {evaluation.strengths.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </div>
        <div>
          <h2>What to improve</h2>
          <ul className={styles.list}>
            {evaluation.improvements.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </div>
      </div>
      <div className={styles.actions}>
        <Button variant="primary" onClick={onContinue}>
          {isLast ? 'See your results' : 'Next question'}
        </Button>
      </div>
    </section>
  );
}
