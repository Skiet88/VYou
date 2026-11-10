import type { ReactNode } from 'react';
import { AnswerInputSchema } from '../../schemas/interview';
import type { InterviewProblem } from '../../hooks/interviewErrorMessage';
import { useZodForm } from '../../hooks/useZodForm';
import { Button } from '../ui/Button';
import { ErrorSummary, type SummaryItem } from '../ui/ErrorSummary';
import { TextArea } from '../ui/TextArea';
import styles from './Interview.module.css';

interface AnswerFormProps {
  busy: boolean;
  problem: InterviewProblem | null;
  /** Changes whenever a new server problem arrives, so the summary takes focus. */
  problemKey: number;
  onSubmit: (answer: string) => void;
  secondaryAction?: ReactNode;
}

/**
 * The typed answer lives only in this form's state (never in browser storage: it is personal
 * content). It survives every failure, because the parent only remounts this form once an
 * answer has actually been accepted.
 */
export function AnswerForm({ busy, problem, problemKey, onSubmit, secondaryAction }: AnswerFormProps) {
  const { values, errors, submitCount, setValue, handleSubmit } = useZodForm(AnswerInputSchema, { answer: '' });

  const items: SummaryItem[] = [
    ...(errors.answer ? [{ fieldId: 'answer', message: errors.answer }] : []),
    ...(problem ? [{ message: problem.message }, ...problem.reasons.map((reason) => ({ fieldId: 'answer', message: reason }))] : []),
  ];

  return (
    <form className={styles.section} noValidate onSubmit={handleSubmit((data) => onSubmit(data.answer))}>
      <ErrorSummary items={items} focusKey={`${submitCount}:${problemKey}`} />
      <TextArea
        id="answer"
        label="Your answer"
        hint="Aim for a few sentences, using a real example where you can."
        rows={8}
        maxLength={10000}
        value={values.answer}
        onValueChange={(value) => setValue('answer', value)}
        error={errors.answer}
      />
      <div className={styles.actions}>
        <Button type="submit" variant="primary" busy={busy} busyLabel="Submitting your answer…">
          Submit answer
        </Button>
        {secondaryAction}
      </div>
    </form>
  );
}
