import { screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { backend, question, scoredTurn } from '../../test/msw/backend';
import { expectNoAxeViolations } from '../../test/a11y';
import { renderApp } from '../../test/renderApp';

describe('Completed interview detail', () => {
  it('shows the summary first and each question collapsed below it', async () => {
    backend.signedInAs();
    const done = backend.addInterview('thabo@example.com', { status: 'COMPLETED', currentQuestion: null });
    done.turns.push(scoredTurn(1, 'I led a migration to Spring Boot 3.'), scoredTurn(2));
    const { user } = renderApp([`/interviews/${done.id}`]);

    expect(await screen.findByText(/you gave concrete examples/i)).toBeInTheDocument();
    expect(screen.getByText(/^Taken /)).toBeInTheDocument();

    const breakdown = screen.getByRole('region', { name: 'Question by question' });
    const firstQuestion = within(breakdown).getByText('Question 1').closest('details')!;
    expect(firstQuestion).not.toHaveAttribute('open');
    expect(within(breakdown).getAllByRole('group')).toHaveLength(2);

    await user.click(within(breakdown).getByText('Question 1'));
    expect(firstQuestion).toHaveAttribute('open');
    expect(within(firstQuestion).getByText(question(1))).toBeVisible();
    expect(within(firstQuestion).getByText('I led a migration to Spring Boot 3.')).toBeVisible();
    expect(within(firstQuestion).getByText('Quantify the impact')).toBeVisible();
  });

  it('has no detectable accessibility violations with a question expanded', async () => {
    backend.signedInAs();
    const done = backend.addInterview('thabo@example.com', { status: 'COMPLETED', currentQuestion: null });
    done.turns.push(scoredTurn(1));
    const { user, container } = renderApp([`/interviews/${done.id}`]);
    await screen.findByText(/you gave concrete examples/i);
    await user.click(screen.getByText('Question 1'));
    await expectNoAxeViolations(container);
  });
});
