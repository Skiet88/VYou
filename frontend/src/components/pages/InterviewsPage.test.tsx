import { screen, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { backend, scoredTurn } from '../../test/msw/backend';
import { server } from '../../test/msw/server';
import { expectNoAxeViolations } from '../../test/a11y';
import { renderApp } from '../../test/renderApp';

const rows = () => within(screen.getByRole('list', { name: 'Your interviews' })).getAllByRole('listitem');

function seedFinished(email: string, jobRole: string) {
  const interview = backend.addInterview(email, { jobRole, status: 'COMPLETED', currentQuestion: null });
  interview.turns.push(scoredTurn(1), scoredTurn(2));
  interview.summary = { averageOverall: 6.5 } as Record<string, unknown>;
  return interview;
}

describe('Interviews list', () => {
  it('lists interviews newest first with status in words and the average score', async () => {
    backend.signedInAs();
    seedFinished('thabo@example.com', 'Data Analyst');
    const unfinished = backend.addInterview('thabo@example.com', { jobRole: 'Cloud Engineer' });
    unfinished.turns.push(scoredTurn(1));
    renderApp(['/interviews']);

    expect(await screen.findByText('2 interviews')).toBeInTheDocument();
    const [first, second] = rows();
    expect(within(first).getByRole('link', { name: 'Cloud Engineer (resume)' })).toHaveAttribute('href', `/interviews/${unfinished.id}`);
    expect(first).toHaveTextContent('In progress, 1 of 5 answered');
    expect(within(second).getByRole('link', { name: 'Data Analyst' })).toBeInTheDocument();
    expect(second).toHaveTextContent('Completed');
    expect(second).toHaveTextContent('6.5/10 out of 10');
  });

  it('filters by status, keeping the filter in the address', async () => {
    backend.signedInAs();
    seedFinished('thabo@example.com', 'Data Analyst');
    backend.addInterview('thabo@example.com', { jobRole: 'Cloud Engineer' });
    const { user } = renderApp(['/interviews']);

    await screen.findByText('2 interviews');
    const filters = screen.getByRole('navigation', { name: 'Filter interviews' });
    expect(within(filters).getByRole('link', { name: 'All' })).toHaveAttribute('aria-current', 'true');

    await user.click(within(filters).getByRole('link', { name: 'Completed' }));
    expect(await screen.findByText('1 interview · completed')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Data Analyst' })).toBeInTheDocument();
    expect(screen.queryByText('Cloud Engineer')).toBeNull();
    expect(within(filters).getByRole('link', { name: 'Completed' })).toHaveAttribute('aria-current', 'true');
  });

  it('pages through long histories with previous/next links', async () => {
    backend.signedInAs();
    for (let i = 0; i < 12; i++) backend.addInterview('thabo@example.com', { jobRole: `Role ${i}` });
    const { user } = renderApp(['/interviews']);

    expect(await screen.findByText('Page 1 of 2')).toBeInTheDocument();
    expect(rows()).toHaveLength(10);
    expect(screen.queryByRole('link', { name: /previous/i })).toBeNull();

    await user.click(screen.getByRole('link', { name: /next/i }));
    expect(await screen.findByText('Page 2 of 2')).toBeInTheDocument();
    expect(rows()).toHaveLength(2);
    expect(screen.getByRole('link', { name: 'Role 0 (resume)' })).toBeInTheDocument();
    expect(document.title).toBe('Interviews, page 2 · VYou');
  });

  it('ignores invalid filter and page values in the address', async () => {
    backend.signedInAs();
    backend.addInterview('thabo@example.com');
    renderApp(['/interviews?status=DELETED&page=-4']);
    expect(await screen.findByText('1 interview')).toBeInTheDocument();
  });

  it('shows a helpful empty state for a new user', async () => {
    backend.signedInAs();
    renderApp(['/interviews']);
    expect(await screen.findByRole('heading', { name: 'You haven’t done any interviews yet' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Start interview' })).toHaveAttribute('href', '/interviews/new');
  });

  it('shows a retryable error state when the list cannot load', async () => {
    backend.signedInAs();
    server.use(http.get('*/api/interviews/mine', () => HttpResponse.error()));
    renderApp(['/interviews']);
    expect(await screen.findByRole('alert')).toHaveTextContent(/could not be loaded/i);
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument();
  });

  it('has no detectable accessibility violations', async () => {
    backend.signedInAs();
    seedFinished('thabo@example.com', 'Data Analyst');
    for (let i = 0; i < 11; i++) backend.addInterview('thabo@example.com');
    const { container } = renderApp(['/interviews']);
    await screen.findByText('Page 1 of 2');
    await expectNoAxeViolations(container);
  });
});

describe('Interviews list while loading a new filter', () => {
  it('never shows the previous filter’s rows under the new filter label', async () => {
    backend.signedInAs();
    for (let i = 0; i < 3; i++) backend.addInterview('thabo@example.com', { jobRole: `Open ${i}` });
    let release: () => void = () => {};
    const gate = new Promise<void>((resolve) => (release = resolve));
    const { user } = renderApp(['/interviews']);
    await screen.findByText('3 interviews');

    server.use(
      http.get('*/api/interviews/mine', async () => {
        await gate;
        return HttpResponse.json({ items: [], page: 0, size: 10, totalItems: 0, totalPages: 0 });
      }),
    );
    await user.click(screen.getByRole('link', { name: 'Completed' }));
    // While the completed list is still loading, the "all" rows must not be presented as completed.
    expect(await screen.findByRole('status')).toHaveTextContent('Loading');
    expect(screen.queryByText('Open 0')).toBeNull();
    release();
    expect(await screen.findByText('You have no completed interviews.')).toBeInTheDocument();
  });
});
