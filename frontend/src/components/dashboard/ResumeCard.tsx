import type { InterviewListItem } from '../../schemas/interview';
import { ButtonLink } from '../ui/ButtonLink';
import styles from './Dashboard.module.css';

/** When an interview is unfinished, resuming it is the screen's one primary action. */
export function ResumeCard({ interview }: { interview: InterviewListItem }) {
  return (
    <section className={[styles.card, styles.highlight].join(' ')} aria-labelledby="resume-heading">
      <h2 id="resume-heading">Continue where you left off</h2>
      <p>
        <strong>{interview.jobRole}</strong>: {interview.questionsAnswered} of {interview.totalQuestions} questions answered.
      </p>
      <div className={styles.actions}>
        <ButtonLink to={`/interviews/${interview.id}`} variant="primary">
          Resume interview
        </ButtonLink>
        <ButtonLink to="/interviews/new">Start a new interview</ButtonLink>
      </div>
    </section>
  );
}
