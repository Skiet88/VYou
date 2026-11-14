import { screen, waitFor, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { backend } from '../../test/msw/backend';
import { expectNoAxeViolations } from '../../test/a11y';
import { renderApp } from '../../test/renderApp';

const location = () => screen.getByTestId('location').textContent;

async function enterCustomRole(user: ReturnType<typeof renderApp>['user'], role: string) {
  await user.click(await screen.findByRole('button', { name: 'Custom…' }));
  await user.type(screen.getByLabelText('Describe the role'), role);
}

async function fillRoleAndLevel(user: ReturnType<typeof renderApp>['user'], role = 'Java Developer', level = /junior/i) {
  await enterCustomRole(user, role);
  await user.click(screen.getByRole('button', { name: 'Continue' }));
  await user.click(await screen.findByRole('radio', { name: level }));
  await user.click(screen.getByRole('button', { name: 'Continue' }));
  await screen.findByRole('heading', { name: 'Check your interview and start' });
}

describe('New interview wizard', () => {
  it('walks through role, level and review, then starts the interview', async () => {
    backend.signedInAs();
    const { user } = renderApp(['/interviews/new']);

    expect(await screen.findByText('Step 1 of 3')).toBeInTheDocument();
    await fillRoleAndLevel(user, '  Data Analyst  ', /mid-level/i);

    const summary = screen.getByText('Role').closest('dl')!;
    expect(within(summary).getByText('Data Analyst')).toBeInTheDocument();
    expect(within(summary).getByText('Mid-level')).toBeInTheDocument();
    expect(screen.getByRole('radio', { name: /after each question/i })).toBeChecked();

    await user.click(screen.getByRole('radio', { name: /at the end/i }));
    await user.click(screen.getByRole('button', { name: 'Start interview' }));

    expect(await screen.findByRole('heading', { name: 'Question 1 of 5' })).toBeInTheDocument();
    expect(location()).toMatch(/^\/interviews\/[0-9a-f-]{36}$/);
    const created = backend.interview(location()!.split('/').pop()!);
    expect(created).toMatchObject({ jobRole: 'Data Analyst', level: 'MID', feedbackMode: 'END_OF_INTERVIEW' });
  });

  it('offers role presets as well as a custom role', async () => {
    backend.signedInAs();
    const { user } = renderApp(['/interviews/new']);

    await user.click(await screen.findByRole('button', { name: 'Software Developer' }));
    await user.click(screen.getByRole('button', { name: 'Continue' }));
    await screen.findByRole('radio', { name: /junior/i });
    await user.click(screen.getByRole('button', { name: 'Back' }));
    expect(screen.getByRole('button', { name: 'Software Developer' })).toHaveAttribute('class', expect.stringContaining('chipActive'));
  });

  it('requires a role and a level before moving on', async () => {
    backend.signedInAs();
    const { user } = renderApp(['/interviews/new']);

    await user.click(await screen.findByRole('button', { name: 'Continue' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Enter the role you are practising for');
    expect(screen.getByText('Step 1 of 3')).toBeInTheDocument();

    await enterCustomRole(user, 'Dev');
    await user.click(screen.getByRole('button', { name: 'Continue' }));
    await user.click(await screen.findByRole('button', { name: 'Continue' }));
    const alert = await screen.findByRole('alert');
    expect(within(alert).getByRole('link', { name: 'Choose your experience level' })).toHaveAttribute('href', '#level');
  });

  it('cannot jump ahead to a step whose earlier answers are missing', async () => {
    backend.signedInAs();
    renderApp(['/interviews/new?step=3']);
    expect(await screen.findByRole('heading', { name: 'What role are you practising for?' })).toBeInTheDocument();
  });

  it('keeps answers when going back, and lets you change them from the review', async () => {
    backend.signedInAs();
    const { user } = renderApp(['/interviews/new']);
    await fillRoleAndLevel(user);

    await user.click(screen.getByRole('button', { name: 'Change role' }));
    expect(await screen.findByLabelText('Describe the role')).toHaveValue('Java Developer');
    await user.click(screen.getByRole('button', { name: 'Continue' }));
    expect(await screen.findByRole('radio', { name: /junior/i })).toBeChecked();
    await user.click(screen.getByRole('button', { name: 'Back' }));
    expect(await screen.findByLabelText('Describe the role')).toHaveValue('Java Developer');
  });

  it('moves focus to the heading of each new step', async () => {
    backend.signedInAs();
    const { user } = renderApp(['/interviews/new']);
    await enterCustomRole(user, 'Dev');
    await user.click(screen.getByRole('button', { name: 'Continue' }));
    await waitFor(() =>
      expect(screen.getByRole('heading', { name: 'What level is the role, and how do you want to answer?' })).toHaveFocus(),
    );
  });

  it('offers to resume an unfinished interview', async () => {
    backend.signedInAs();
    const unfinished = backend.addInterview('thabo@example.com', { jobRole: 'Cloud Engineer' });
    renderApp(['/interviews/new']);

    const resume = await screen.findByRole('link', { name: 'Resume it' });
    expect(resume).toHaveAttribute('href', `/interviews/${unfinished.id}`);
    expect(screen.getByText('Cloud Engineer')).toBeInTheDocument();
  });

  it('shows voice and video as coming soon, with only text selectable', async () => {
    backend.signedInAs();
    const { user } = renderApp(['/interviews/new']);
    await enterCustomRole(user, 'Java Developer');
    await user.click(screen.getByRole('button', { name: 'Continue' }));

    expect(await screen.findByText('Text')).toBeInTheDocument();
    expect(screen.getByText('Voice')).toBeInTheDocument();
    expect(screen.getByText('Video')).toBeInTheDocument();
    expect(screen.getAllByText(/coming soon/i)).toHaveLength(2);
    expect(screen.queryByRole('radio', { name: /voice|video/i })).toBeNull();
    expect(screen.queryByRole('button', { name: /^voice$|^video$/i })).toBeNull();
  });

  it('keeps every choice when the AI is unavailable, and can retry', async () => {
    backend.signedInAs();
    backend.failAi(1);
    const { user } = renderApp(['/interviews/new']);
    await fillRoleAndLevel(user);

    await user.click(screen.getByRole('button', { name: 'Start interview' }));
    expect(await screen.findByRole('alert')).toHaveTextContent(/AI interviewer is unavailable/i);
    expect(screen.getByText('Java Developer')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Start interview' }));
    expect(await screen.findByRole('heading', { name: 'Question 1 of 5' })).toBeInTheDocument();
  });

  it('has no detectable accessibility violations on any step', async () => {
    backend.signedInAs();
    const { user, container } = renderApp(['/interviews/new']);
    await screen.findByRole('button', { name: 'Custom…' });
    await expectNoAxeViolations(container);
    await enterCustomRole(user, 'Dev');
    await user.click(screen.getByRole('button', { name: 'Continue' }));
    await screen.findByRole('radio', { name: /junior/i });
    await expectNoAxeViolations(container);
    await user.click(screen.getByRole('radio', { name: /junior/i }));
    await user.click(screen.getByRole('button', { name: 'Continue' }));
    await screen.findByRole('button', { name: 'Start interview' });
    await expectNoAxeViolations(container);
  });
});
