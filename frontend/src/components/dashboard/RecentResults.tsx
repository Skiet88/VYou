import { Link } from 'react-router-dom';
import type { InterviewListItem } from '../../schemas/interview';
import { InterviewList } from '../interview/InterviewList';
import styles from './Dashboard.module.css';

export const RECENT_RESULTS = 3;

export function RecentResults({ items }: { items: InterviewListItem[] }) {
  return (
    <section aria-labelledby="recent-heading" className={styles.stack}>
      <div className={styles.sectionHeader}>
        <h2 id="recent-heading">Recent results</h2>
        <Link to="/interviews?status=COMPLETED">See all completed interviews</Link>
      </div>
      <InterviewList items={items.slice(0, RECENT_RESULTS)} label="Recent results" />
    </section>
  );
}
