import type { ExperienceLevel, FeedbackMode } from '../../schemas/interview';
import type { RadioOption } from '../ui/RadioGroup';

export const LEVEL_LABELS: Record<ExperienceLevel, string> = {
  INTERN: 'Intern',
  JUNIOR: 'Junior',
  MID: 'Mid-level',
  SENIOR: 'Senior',
};

export const LEVEL_OPTIONS: RadioOption[] = [
  { value: 'INTERN', label: 'Intern', hint: 'Internship, graduate programme or work-integrated learning' },
  { value: 'JUNIOR', label: 'Junior', hint: 'Up to about 2 years of experience' },
  { value: 'MID', label: 'Mid-level', hint: 'About 2 to 5 years of experience' },
  { value: 'SENIOR', label: 'Senior', hint: 'More than 5 years of experience' },
];

export const FEEDBACK_LABELS: Record<FeedbackMode, string> = {
  INSTANT: 'After each question',
  END_OF_INTERVIEW: 'At the end',
};

export const FEEDBACK_OPTIONS: RadioOption[] = [
  { value: 'INSTANT', label: FEEDBACK_LABELS.INSTANT, hint: 'Best for practice: see what to improve straight away.' },
  { value: 'END_OF_INTERVIEW', label: FEEDBACK_LABELS.END_OF_INTERVIEW, hint: 'Closer to a real interview: your scores appear when you finish.' },
];

export const ROLE_PRESETS = [
  'Software Developer',
  'Data Analyst',
  'DevOps Engineer',
  'UI/UX Designer',
  'Product Manager',
  'QA Engineer',
];

export interface ModeOption {
  value: 'TEXT' | 'VOICE' | 'VIDEO';
  label: string;
  hint: string;
  available: boolean;
}

export const MODE_OPTIONS: ModeOption[] = [
  { value: 'TEXT', label: 'Text', hint: 'Type each answer.', available: true },
  { value: 'VOICE', label: 'Voice', hint: 'Coming soon: speak your answers.', available: false },
  { value: 'VIDEO', label: 'Video', hint: 'Coming soon: a full video simulation.', available: false },
];
