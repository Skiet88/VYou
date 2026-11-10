import { Link } from 'react-router-dom';
import type { InterviewListItem } from '../../schemas/interview';
import { formatDate } from '../../util/format';
import { ScoreValue } from '../ui/ScoreList';
import { LEVEL_LABELS } from './labels';
import styles from './InterviewList.module.css';

/** Status is always spelled out in text, never shown by colour alone. */
function StatusText({ item }: { item: InterviewListItem }) {
  if (item.status === 'COMPLETED') return <span>Completed</span>;
  return (
    <span>
      In progress, {item.questionsAnswered} of {item.totalQuestions} answered
    </span>
  );
}

export function InterviewList({ items, label = 'Your interviews' }: { items: InterviewListItem[]; label?: string }) {
  return (
    <ul className={styles.list} aria-label={label}>
      {items.map((item) => (
        <li key={item.id} className={styles.row}>
          <div className={styles.main}>
            <Link
              to={`/interviews/${item.id}`}
              className={styles.title}
              aria-label={item.status === 'IN_PROGRESS' ? `${item.jobRole} (resume)` : undefined}
            >
              {item.jobRole}
            </Link>
            <p className={styles.meta}>
              {LEVEL_LABELS[item.level]} · {formatDate(item.startedAt)} · <StatusText item={item} />
            </p>
          </div>
          <div className={styles.score}>
            {item.averageOverall !== null ? (
              <>
                <span className={styles.scoreValue}>
                  <ScoreValue value={item.averageOverall} decimals={1} />
                </span>
                <span className={styles.scoreLabel}>average</span>
              </>
            ) : item.status === 'IN_PROGRESS' ? (
              <span className={styles.scoreLabel}>Resume</span>
            ) : (
              <span className={styles.scoreLabel}>No score</span>
            )}
          </div>
        </li>
      ))}
    </ul>
  );
}
