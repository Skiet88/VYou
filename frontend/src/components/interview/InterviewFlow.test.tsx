import { screen, waitFor, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { backend, question } from '../../test/msw/backend';
import { expectNoAxeViolations } from '../../test/a11y';
import { renderApp } from '../../test/renderApp';

const GOOD = 'I built a Spring Boot REST API with JPA and wrote JUnit tests for it.';

async function answer(user: ReturnType<typeof renderApp>['user'], text: string) {
  const box = await screen.findByLabelText('Your answer');
  await user.clear(box);
  await user.type(box, text);
  await user.click(screen.getByRole('button', { name: 'Submit answer' }));
}

describe('Interview flow', () => {
  it('practice mode: feedback after every answer, then results with a generated summary', async () => {
    backend.signedInAs();
    const interview = backend.addInterview('thabo@example.com', { feedbackMode: 'INSTANT' });
    const { user } = renderApp([`/interviews/${interview.id}`]);

    for (let n = 1; n <= 5; n++) {
      expect(await screen.findByRole('heading', { name: `Question ${n} of 5` })).toBeInTheDocument();
      expect(screen.getByText(question(n))).toBeInTheDocument();
      await answer(user, `${GOOD} (${n})`);

      expect(await screen.findByRole('heading', { name: `Feedback on question ${n}` })).toBeInTheDocument();
      expect(screen.getByText('Quantify the impact')).toBeInTheDocument();
      expect(screen.getByText('Clarity').closest('div')).toHaveTextContent('8/10 out of 10');
      await user.click(screen.getByRole('button', { name: n < 5 ? 'Next question' : 'See your results' }));
    }

    expect(await screen.findByRole('heading', { name: 'Interview complete' })).toBeInTheDocument();
    expect(await screen.findByText(/you gave concrete examples/i)).toBeInTheDocument();
    expect(screen.getByText(/5 of 5 questions answered/)).toBeInTheDocument();
    expect(backend.summaryRequests).toBe(1);
  });

  it('realistic mode: no scores until the end, just a saved confirmation', async () => {
    backend.signedInAs();
    const interview = backend.addInterview('thabo@example.com', { feedbackMode: 'END_OF_INTERVIEW' });
    const { user } = renderApp([`/interviews/${interview.id}`]);

    await answer(user, GOOD);
    expect(await screen.findByRole('status')).toHaveTextContent('Answer 1 saved');
    expect(screen.getByRole('heading', { name: 'Question 2 of 5' })).toBeInTheDocument();
    expect(screen.queryByText(/feedback on question/i)).toBeNull();
    expect(screen.queryByText('Quantify the impact')).toBeNull();
    expect(screen.getByLabelText('Your answer')).toHaveValue('');
  });

  it('a rejected answer explains why and keeps the typed text', async () => {
    backend.signedInAs();
    const interview = backend.addInterview('thabo@example.com');
    const { user } = renderApp([`/interviews/${interview.id}`]);

    await answer(user, 'Not sure really');
    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Your answer could not be used yet.');
    expect(within(alert).getByRole('link', { name: 'Answer is too short (3 words, minimum 5)' })).toHaveAttribute('href', '#answer');
    expect(screen.getByLabelText('Your answer')).toHaveValue('Not sure really');
    await waitFor(() => expect(alert).toHaveFocus());
  });

  it('when the AI is unavailable the answer is kept and a retry succeeds', async () => {
    backend.signedInAs();
    backend.failAi(1);
    const interview = backend.addInterview('thabo@example.com');
    const { user } = renderApp([`/interviews/${interview.id}`]);

    await answer(user, GOOD);
    expect(await screen.findByRole('alert')).toHaveTextContent(/your answer is still here/i);
    expect(screen.getByLabelText('Your answer')).toHaveValue(GOOD);

    await user.click(screen.getByRole('button', { name: 'Submit answer' }));
    expect(await screen.findByRole('heading', { name: 'Feedback on question 1' })).toBeInTheDocument();
  });

  it('validates an empty answer without calling the API', async () => {
    backend.signedInAs();
    const interview = backend.addInterview('thabo@example.com');
    const { user } = renderApp([`/interviews/${interview.id}`]);
    await user.click(await screen.findByRole('button', { name: 'Submit answer' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Write your answer before submitting');
    expect(backend.interview(interview.id)?.turns).toHaveLength(0);
  });

  it('ending early asks for confirmation, then shows the summary', async () => {
    backend.signedInAs();
    const interview = backend.addInterview('thabo@example.com');
    const { user } = renderApp([`/interviews/${interview.id}`]);

    await answer(user, GOOD);
    await user.click(await screen.findByRole('button', { name: 'Next question' }));
    await user.click(await screen.findByRole('button', { name: 'End interview' }));

    const dialog = await screen.findByRole('dialog', { name: 'End this interview now?' });
    expect(dialog).toHaveTextContent("You've answered 1 of 5 questions");
    expect(within(dialog).getByRole('button', { name: 'Keep going' })).toHaveFocus();

    await user.click(within(dialog).getByRole('button', { name: 'End interview' }));
    expect(await screen.findByRole('heading', { name: 'Interview complete' })).toBeInTheDocument();
    expect(await screen.findByText(/you gave concrete examples/i)).toBeInTheDocument();
    expect(screen.getByText(/1 of 5 questions answered/)).toBeInTheDocument();
  });

  it('"Keep going" closes the confirmation without ending anything', async () => {
    backend.signedInAs();
    const interview = backend.addInterview('thabo@example.com');
    const { user } = renderApp([`/interviews/${interview.id}`]);
    await user.click(await screen.findByRole('button', { name: 'End interview' }));
    await user.click(await screen.findByRole('button', { name: 'Keep going' }));
    expect(backend.interview(interview.id)?.status).toBe('IN_PROGRESS');
  });

  it('ending before answering anything says there is nothing to summarise', async () => {
    backend.signedInAs();
    const interview = backend.addInterview('thabo@example.com');
    const { user } = renderApp([`/interviews/${interview.id}`]);
    await user.click(await screen.findByRole('button', { name: 'End interview' }));
    const dialog = await screen.findByRole('dialog');
    expect(dialog).toHaveTextContent("won't be a summary");
    await user.click(within(dialog).getByRole('button', { name: 'End interview' }));
    expect(await screen.findByText(/nothing to summarise/i)).toBeInTheDocument();
  });

  it('a summary that fails to generate can be retried', async () => {
    backend.signedInAs();
    const finished = backend.addInterview('thabo@example.com', { status: 'COMPLETED', currentQuestion: null });
    finished.turns.push({
      sequenceNumber: 1,
      question: question(1),
      answer: GOOD,
      evaluation: { clarity: 8, technical: 7, confidence: 6, overall: 7, strengths: ['Concrete example'], improvements: ['Quantify the impact'] },
      answeredAt: new Date().toISOString(),
    });
    backend.failAi(1);
    const { user } = renderApp([`/interviews/${finished.id}`]);

    expect(await screen.findByText(/could not be prepared/i)).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Try again' }));
    expect(await screen.findByText(/you gave concrete examples/i)).toBeInTheDocument();
    expect(backend.summaryRequests).toBe(2);
  });

  it('realistic mode reveals the scores once the last answer completes the interview', async () => {
    backend.signedInAs();
    const interview = backend.addInterview('thabo@example.com', { feedbackMode: 'END_OF_INTERVIEW' });
    const { user } = renderApp([`/interviews/${interview.id}`]);
    for (let n = 1; n <= 5; n++) {
      await screen.findByRole('heading', { name: `Question ${n} of 5` });
      await answer(user, `${GOOD} (${n})`);
    }
    expect(await screen.findByRole('heading', { name: 'Interview complete' })).toBeInTheDocument();
    expect(await screen.findByText(/you gave concrete examples/i)).toBeInTheDocument();
  });

  it("someone else's interview is reported as not found", async () => {
    backend.addUser('owner@example.com', 'Secret123');
    const theirs = backend.addInterview('owner@example.com');
    backend.signedInAs();
    renderApp([`/interviews/${theirs.id}`]);
    expect(await screen.findByRole('heading', { name: 'Interview not found' })).toBeInTheDocument();
  });

  it('has no detectable accessibility violations: question, feedback and results', async () => {
    backend.signedInAs();
    const interview = backend.addInterview('thabo@example.com');
    const { user, container } = renderApp([`/interviews/${interview.id}`]);
    await screen.findByRole('heading', { name: 'Question 1 of 5' });
    await expectNoAxeViolations(container);

    await answer(user, GOOD);
    await screen.findByRole('heading', { name: 'Feedback on question 1' });
    await expectNoAxeViolations(container);

    await user.click(screen.getByRole('button', { name: 'Next question' }));
    await user.click(await screen.findByRole('button', { name: 'End interview' }));
    await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'End interview' }));
    await screen.findByText(/you gave concrete examples/i);
    await expectNoAxeViolations(container);
  });
});
