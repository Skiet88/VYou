import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { backend, scoredTurn } from '../../test/msw/backend';
import { server } from '../../test/msw/server';
import { expectNoAxeViolations } from '../../test/a11y';
import { renderApp } from '../../test/renderApp';

function finished(jobRole: string, averageOverall: number | null) {
  const i = backend.addInterview('thabo@example.com', { jobRole, status: 'COMPLETED', currentQuestion: null });
  i.turns.push(scoredTurn(1));
  i.summary = averageOverall === null ? null : ({ averageOverall } as Record<string, unknown>);
  return i;
}

describe('Dashboard', () => {
  it('a new user gets one clear way to start', async () => {
    backend.signedInAs();
    renderApp(['/']);
    expect(await screen.findByRole('heading', { name: 'Your first interview' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Start interview' })).toHaveAttribute('href', '/interviews/new');
    expect(screen.queryByRole('heading', { name: 'Your progress' })).toBeNull();
    expect(screen.queryByRole('heading', { name: 'Recent results' })).toBeNull();
  });

  it('an unfinished interview makes resuming the primary action', async () => {
    backend.signedInAs();
    const open = backend.addInterview('thabo@example.com', { jobRole: 'Cloud Engineer' });
    open.turns.push(scoredTurn(1), scoredTurn(2));
    renderApp(['/']);

    const card = await screen.findByRole('region', { name: 'Continue where you left off' });
    expect(card).toHaveTextContent('Cloud Engineer: 2 of 5 questions answered');
    expect(within(card).getByRole('link', { name: 'Resume interview' })).toHaveAttribute('href', `/interviews/${open.id}`);
    expect(within(card).getByRole('link', { name: 'Start a new interview' })).toHaveAttribute('href', '/interviews/new');
    expect(screen.queryByRole('link', { name: 'Start interview' })).toBeNull();
  });

  it('shows progress and the three most recent results', async () => {
    backend.signedInAs();
    finished('Role A', 5);
    finished('Role B', 6);
    finished('Role C', null);
    finished('Role D', 8);
    renderApp(['/']);

    const progress = await screen.findByRole('region', { name: 'Your progress' });
    expect(within(progress).getByText('Interviews completed').nextSibling).toHaveTextContent('4');
    // Average of the scored ones among the latest five: (5 + 6 + 8) / 3 = 6.3
    expect(progress).toHaveTextContent('6.3/10 out of 10');
    expect(progress).toHaveTextContent('Across your last 3 scored interviews');

    const recent = screen.getByRole('list', { name: 'Recent results' });
    expect(within(recent).getAllByRole('listitem')).toHaveLength(3);
    expect(within(recent).getAllByRole('link').map((a) => a.textContent)).toEqual(['Role D', 'Role C', 'Role B']);
    expect(screen.getByRole('link', { name: 'See all completed interviews' })).toHaveAttribute('href', '/interviews?status=COMPLETED');
    expect(screen.getByRole('heading', { name: 'Ready for another?' })).toBeInTheDocument();
  });

  it('shows a retryable error when the dashboard data cannot load', async () => {
    backend.signedInAs();
    server.use(http.get('*/api/interviews/mine', () => HttpResponse.error()));
    renderApp(['/']);
    expect(await screen.findByRole('alert')).toHaveTextContent(/could not be loaded/i);
  });

  it('has no detectable accessibility violations', async () => {
    backend.signedInAs();
    backend.addInterview('thabo@example.com');
    finished('Role A', 7);
    const { container } = renderApp(['/']);
    await screen.findByRole('region', { name: 'Your progress' });
    await expectNoAxeViolations(container);
  });
});
