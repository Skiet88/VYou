import { screen, waitFor, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { adminBackend, backend, scoredTurn } from '../../test/msw/backend';
import { expectNoAxeViolations } from '../../test/a11y';
import { renderApp } from '../../test/renderApp';

const usersList = () => screen.getByRole('list', { name: 'Users' });
const rowFor = (name: string) => within(usersList()).getByText(name).closest('li')!;

function seedPeople() {
  backend.addUser('lerato@example.com', 'Secret123', 'Lerato Mokoena');
  backend.addUser('sipho@example.com', 'Secret123', 'Sipho Dlamini');
  backend.addUser('other.admin@example.com', 'Secret123', 'Other Admin', 'ADMIN');
  const done = backend.addInterview('lerato@example.com', { status: 'COMPLETED', currentQuestion: null });
  done.turns.push(scoredTurn(1));
  done.summary = { averageOverall: 6.5 } as Record<string, unknown>;
  backend.addInterview('lerato@example.com');
  backend.signedInAs('admin@example.com', 'Naledi Khumalo', 'ADMIN');
}

describe('Users (admin)', () => {
  it('lists users with roles, statuses and aggregate numbers in words', async () => {
    seedPeople();
    renderApp(['/users']);

    await screen.findByText('4 users');
    const lerato = rowFor('Lerato Mokoena');
    expect(lerato).toHaveTextContent('lerato@example.com');
    expect(lerato).toHaveTextContent('2 interviews, 1 completed · average 6.5 out of 10');
    expect(rowFor('Sipho Dlamini')).toHaveTextContent('No interviews yet');
    expect(rowFor('Other Admin')).toHaveTextContent('Admin');
    expect(rowFor('Naledi Khumalo')).toHaveTextContent('You');
  });

  it('offers no action on your own row or on other admins', async () => {
    seedPeople();
    renderApp(['/users']);
    await screen.findByText('4 users');
    expect(within(rowFor('Naledi Khumalo')).queryByRole('button')).toBeNull();
    expect(within(rowFor('Other Admin')).queryByRole('button')).toBeNull();
    expect(within(rowFor('Sipho Dlamini')).getByRole('button', { name: 'Suspend Sipho Dlamini' })).toBeInTheDocument();
  });

  it('suspending asks for confirmation, then updates the row and announces it', async () => {
    seedPeople();
    const { user } = renderApp(['/users']);
    await screen.findByText('4 users');

    await user.click(screen.getByRole('button', { name: 'Suspend Sipho Dlamini' }));
    const dialog = await screen.findByRole('dialog', { name: 'Suspend Sipho Dlamini?' });
    expect(dialog).toHaveTextContent('signed out straight away');
    expect(within(dialog).getByRole('button', { name: 'Cancel' })).toHaveFocus();

    await user.click(within(dialog).getByRole('button', { name: 'Suspend' }));
    await waitFor(() => expect(adminBackend.status('sipho@example.com')).toBe('SUSPENDED'));
    expect(await within(usersList()).findByRole('button', { name: 'Reinstate Sipho Dlamini' })).toBeInTheDocument();
    expect(rowFor('Sipho Dlamini')).toHaveTextContent('Suspended');
    expect(screen.getAllByRole('status').some((s) => s.textContent === 'Sipho Dlamini suspended.')).toBe(true);

    await user.click(screen.getByRole('button', { name: 'Reinstate Sipho Dlamini' }));
    await waitFor(() => expect(adminBackend.status('sipho@example.com')).toBe('ACTIVE'));
  });

  it('cancelling the confirmation changes nothing', async () => {
    seedPeople();
    const { user } = renderApp(['/users']);
    await screen.findByText('4 users');
    await user.click(screen.getByRole('button', { name: 'Suspend Sipho Dlamini' }));
    await user.click(await screen.findByRole('button', { name: 'Cancel' }));
    expect(adminBackend.status('sipho@example.com')).toBe('ACTIVE');
  });

  it('searches by name or email and filters by status, all in the address', async () => {
    seedPeople();
    adminBackend.suspend('sipho@example.com');
    const { user } = renderApp(['/users']);
    await screen.findByText('4 users');

    await user.type(screen.getByLabelText('Search by name or email'), 'lerato');
    await user.click(screen.getByRole('button', { name: 'Search' }));
    expect(await screen.findByText('1 user matching “lerato”')).toBeInTheDocument();

    await user.click(within(screen.getByRole('navigation', { name: 'Filter users' })).getByRole('link', { name: 'Suspended' }));
    expect(await screen.findByText('No users match.')).toBeInTheDocument();
  });

  it('is not reachable for non-admins', async () => {
    backend.signedInAs('user@example.com', 'Plain User');
    renderApp(['/users']);
    expect(await screen.findByRole('heading', { name: 'Page not found' })).toBeInTheDocument();
  });

  it('has no detectable accessibility violations, including the confirmation', async () => {
    seedPeople();
    const { user, container } = renderApp(['/users']);
    await screen.findByText('4 users');
    await expectNoAxeViolations(container);
    await user.click(screen.getByRole('button', { name: 'Suspend Sipho Dlamini' }));
    await screen.findByRole('dialog');
    await expectNoAxeViolations(container);
  });
});
