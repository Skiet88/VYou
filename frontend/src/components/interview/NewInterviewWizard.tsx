import { useEffect, useRef, useState, type ReactNode } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { FeedbackStepSchema, LevelStepSchema, RoleStepSchema, type ExperienceLevel, type FeedbackMode } from '../../schemas/interview';
import { interviewProblem } from '../../hooks/interviewErrorMessage';
import { useMyInterviews, useStartInterview } from '../../hooks/useInterviews';
import { useZodForm } from '../../hooks/useZodForm';
import { Button } from '../ui/Button';
import { ErrorSummary, type SummaryItem } from '../ui/ErrorSummary';
import { RadioGroup } from '../ui/RadioGroup';
import { TextField } from '../ui/TextField';
import { FEEDBACK_OPTIONS, LEVEL_LABELS, LEVEL_OPTIONS, MODE_OPTIONS, ROLE_PRESETS } from './labels';
import { useToast } from '../../hooks/useToast';
import styles from './Wizard.module.css';

interface Choices {
  jobRole: string;
  level: ExperienceLevel | '';
  feedbackMode: FeedbackMode;
}

const TOTAL_STEPS = 3;

/**
 * One question per screen. The step lives in the URL (?step=2) so the browser's Back button
 * moves between steps as people expect; the answers live in component state and are never
 * written to storage. A step can't be reached before the ones it depends on are filled in.
 */
export function NewInterviewWizard() {
  const [params, setParams] = useSearchParams();
  const [choices, setChoices] = useState<Choices>({ jobRole: '', level: '', feedbackMode: 'INSTANT' });

  const requested = Number(params.get('step')) || 1;
  const furthestAllowed = !choices.jobRole ? 1 : !choices.level ? 2 : 3;
  const step = Math.min(Math.max(requested, 1), furthestAllowed);
  const goTo = (next: number) => setParams({ step: String(next) });

  const headingRef = useRef<HTMLHeadingElement>(null);
  const isFirstRender = useRef(true);
  useEffect(() => {
    // Moving between steps doesn't change the path, so announce the new step explicitly.
    if (isFirstRender.current) {
      isFirstRender.current = false;
      return;
    }
    headingRef.current?.focus();
  }, [step]);

  const header = (title: string) => (
    <div className={styles.stepHeader}>
      <p className={styles.stepCount}>
        Step {step} of {TOTAL_STEPS}
      </p>
      <h1 ref={headingRef} tabIndex={-1}>
        {title}
      </h1>
    </div>
  );

  if (step === 1) {
    return (
      <RoleStep
        header={header('What role are you practising for?')}
        initial={choices.jobRole}
        onNext={(jobRole) => {
          setChoices((c) => ({ ...c, jobRole }));
          goTo(2);
        }}
      />
    );
  }
  if (step === 2) {
    return (
      <LevelStep
        header={header('What level is the role, and how do you want to answer?')}
        initial={choices.level}
        onBack={() => goTo(1)}
        onNext={(level) => {
          setChoices((c) => ({ ...c, level }));
          goTo(3);
        }}
      />
    );
  }
  return (
    <ReviewStep
      header={header('Check your interview and start')}
      choices={choices as Choices & { level: ExperienceLevel }}
      onFeedbackModeChange={(feedbackMode) => setChoices((c) => ({ ...c, feedbackMode }))}
      onBack={() => goTo(2)}
      onChange={goTo}
    />
  );
}

function ResumePrompt() {
  const { data } = useMyInterviews({ status: 'IN_PROGRESS', size: 1 });
  const inProgress = data?.items[0];
  if (!inProgress) return null;
  return (
    <div className={styles.resume}>
      <p>
        You have an unfinished interview: <strong>{inProgress.jobRole}</strong>, {inProgress.questionsAnswered} of{' '}
        {inProgress.totalQuestions} questions answered. <Link to={`/interviews/${inProgress.id}`}>Resume it</Link>, or start a
        new one below.
      </p>
    </div>
  );
}

function RoleStep({ header, initial, onNext }: { header: ReactNode; initial: string; onNext: (jobRole: string) => void }) {
  const { values, errors, submitCount, setValue, handleSubmit } = useZodForm(RoleStepSchema, { jobRole: initial });
  const [custom, setCustom] = useState(initial !== '' && !ROLE_PRESETS.includes(initial));
  const items: SummaryItem[] = errors.jobRole ? [{ fieldId: 'jobRole', message: errors.jobRole }] : [];

  return (
    <form className={styles.step} noValidate onSubmit={handleSubmit((data) => onNext(data.jobRole))}>
      {header}
      <ResumePrompt />
      <ErrorSummary items={items} focusKey={submitCount} />
      <div className={styles.chipField}>
        <span className={styles.chipLabel}>Job role</span>
        <div className={styles.chips}>
          {ROLE_PRESETS.map((role) => (
            <button
              key={role}
              type="button"
              className={[styles.chip, !custom && values.jobRole === role ? styles.chipActive : ''].join(' ')}
              onClick={() => {
                setCustom(false);
                setValue('jobRole', role);
              }}
            >
              {role}
            </button>
          ))}
          <button
            type="button"
            className={[styles.chip, custom ? styles.chipActive : ''].join(' ')}
            onClick={() => {
              setCustom(true);
              setValue('jobRole', '');
            }}
          >
            Custom…
          </button>
        </div>
      </div>
      {custom && (
        <TextField
          id="jobRole"
          label="Describe the role"
          hint="For example, Java Developer, Data Analyst or IT Support Technician."
          autoComplete="organization-title"
          value={values.jobRole}
          onValueChange={(value) => setValue('jobRole', value)}
          error={errors.jobRole}
        />
      )}
      <div className={styles.actions}>
        <Button type="submit" variant="primary">
          Continue
        </Button>
      </div>
    </form>
  );
}

function LevelStep({
  header,
  initial,
  onBack,
  onNext,
}: {
  header: ReactNode;
  initial: string;
  onBack: () => void;
  onNext: (level: ExperienceLevel) => void;
}) {
  const { values, errors, submitCount, setValue, handleSubmit } = useZodForm(LevelStepSchema, { level: initial });
  const items: SummaryItem[] = errors.level ? [{ fieldId: 'level', message: errors.level }] : [];
  return (
    <form className={styles.step} noValidate onSubmit={handleSubmit((data) => onNext(data.level as ExperienceLevel))}>
      {header}
      <ErrorSummary items={items} focusKey={submitCount} />
      <RadioGroup
        name="level"
        legend="Experience level"
        options={LEVEL_OPTIONS}
        value={values.level}
        onValueChange={(value) => setValue('level', value)}
        error={errors.level}
      />
      <div className={styles.modeField}>
        <span className={styles.chipLabel}>Interview mode</span>
        <div className={styles.modeCards}>
          {MODE_OPTIONS.map((mode) => (
            <div
              key={mode.value}
              className={[styles.modeCard, mode.value === 'TEXT' ? styles.modeCardActive : '', !mode.available ? styles.modeCardDisabled : ''].join(
                ' ',
              )}
              aria-disabled={!mode.available || undefined}
            >
              <span className={styles.modeCardLabel}>{mode.label}</span>
              <span className={styles.modeCardHint}>{mode.hint}</span>
            </div>
          ))}
        </div>
      </div>
      <div className={styles.actions}>
        <Button type="submit" variant="primary">
          Continue
        </Button>
        <Button onClick={onBack}>Back</Button>
      </div>
    </form>
  );
}

function ReviewStep({
  header,
  choices,
  onFeedbackModeChange,
  onBack,
  onChange,
}: {
  header: ReactNode;
  choices: Choices & { level: ExperienceLevel };
  onFeedbackModeChange: (mode: FeedbackMode) => void;
  onBack: () => void;
  onChange: (step: number) => void;
}) {
  const navigate = useNavigate();
  const start = useStartInterview();
  const { showToast } = useToast();
  const { values, submitCount, setValue, handleSubmit } = useZodForm(FeedbackStepSchema, { feedbackMode: choices.feedbackMode });
  const problem = start.isError ? interviewProblem(start.error) : null;
  const items: SummaryItem[] = problem ? [{ message: problem.message }] : [];

  return (
    <form
      className={styles.step}
      noValidate
      onSubmit={handleSubmit(({ feedbackMode }) =>
        start.mutate(
          { jobRole: choices.jobRole, level: choices.level, feedbackMode },
          {
            onSuccess: (session) => {
              showToast('Interview started.', 'success');
              navigate(`/interviews/${session.id}`);
            },
          },
        ),
      )}
    >
      {header}
      <ErrorSummary items={items} focusKey={`${submitCount}:${start.isError ? 'server' : ''}`} />
      <dl className={styles.summary}>
        <div className={styles.summaryRow}>
          <dt>Role</dt>
          <dd>{choices.jobRole}</dd>
          <dd>
            <button type="button" className={styles.changeLink} aria-label="Change role" onClick={() => onChange(1)}>
              Change
            </button>
          </dd>
        </div>
        <div className={styles.summaryRow}>
          <dt>Level</dt>
          <dd>{LEVEL_LABELS[choices.level]}</dd>
          <dd>
            <button type="button" className={styles.changeLink} aria-label="Change level" onClick={() => onChange(2)}>
              Change
            </button>
          </dd>
        </div>
      </dl>
      <p className={styles.muted}>A short interview of about 15 minutes, one question at a time. You can end it early at any point.</p>
      <RadioGroup
        name="feedbackMode"
        legend="When do you want feedback?"
        options={FEEDBACK_OPTIONS}
        value={values.feedbackMode}
        onValueChange={(value) => {
          setValue('feedbackMode', value);
          onFeedbackModeChange(value as FeedbackMode);
        }}
      />
      <div className={styles.actions}>
        <Button type="submit" variant="primary" busy={start.isPending} busyLabel="Preparing your first question…">
          Start interview
        </Button>
        <Button onClick={onBack} disabled={start.isPending}>
          Back
        </Button>
      </div>
    </form>
  );
}
