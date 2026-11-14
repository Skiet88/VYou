import type { InterviewPage } from '../../schemas/interview';
import { Icon } from '../ui/Icon';
import { ScoreValue } from '../ui/ScoreList';
import styles from './Dashboard.module.css';

/**
 * A two-number glance, nothing more. The average is labelled with exactly what it covers (the
 * most recent summarised interviews), so it never claims more than the data supports.
 */
export function ProgressSummary({ completed }: { completed: InterviewPage }) {
  const scored = completed.items.map((i) => i.averageOverall).filter((v): v is number => v !== null);
  const recentAverage = scored.length ? Math.round((scored.reduce((a, b) => a + b, 0) / scored.length) * 10) / 10 : null;

  return (
    <section aria-labelledby="progress-heading" className={styles.stack}>
      <h2 id="progress-heading">Your progress</h2>
      <dl className={styles.stats}>
        <div className={styles.stat}>
          <dt>
            <span className={[styles.statIcon, styles.statIconSuccess].join(' ')}>
              <Icon name="check" />
            </span>
            Interviews completed
          </dt>
          <dd>{completed.totalItems}</dd>
        </div>
        <div className={styles.stat}>
          <dt>
            <span className={styles.statIcon}>
              <Icon name="chart" />
            </span>
            Recent average
          </dt>
          <dd>
            {recentAverage === null ? '—' : <ScoreValue value={recentAverage} decimals={1} />}
            <span className={styles.statNote}>
              {recentAverage === null
                ? 'Appears once an interview has a summary'
                : `Across your last ${scored.length} scored ${scored.length === 1 ? 'interview' : 'interviews'}`}
            </span>
          </dd>
        </div>
      </dl>
    </section>
  );
}
