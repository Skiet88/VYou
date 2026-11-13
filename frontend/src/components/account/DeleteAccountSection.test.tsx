import { screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { backend } from '../../test/msw/backend';
import { expectNoAxeViolations } from '../../test/a11y';
import { renderApp } from '../../test/renderApp';

describe('Delete account', () => {
  it('requires the password before confirming, then deletes and signs out with a notice', async () => {
    backend.signedInAs('thabo@example.com', 'Thabo');
    const { user } = renderApp(['/account']);

    await user.click(await screen.findByRole('button', { name: 'Delete account' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Enter your password');
    expect(screen.queryByRole('dialog')).toBeNull();

    await user.type(screen.getByLabelText('Confirm your password'), 'Secret123');
    await user.click(screen.getByRole('button', { name: 'Delete account' }));

    const dialog = await screen.findByRole('dialog', { name: 'Delete your account?' });
    expect(dialog).toHaveTextContent('30 days');
    await user.click(within(dialog).getByRole('button', { name: 'Delete account' }));

    expect(await screen.findByText(/will be deleted in 30 days/i)).toBeInTheDocument();
    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
    expect(backend.signedInUserId).toBeNull();
  });

  it('cancelling the confirmation deletes nothing', async () => {
    backend.signedInAs('thabo@example.com', 'Thabo');
    const { user } = renderApp(['/account']);
    await user.type(await screen.findByLabelText('Confirm your password'), 'Secret123');
    await user.click(screen.getByRole('button', { name: 'Delete account' }));
    await user.click(await screen.findByRole('button', { name: 'Cancel' }));
    expect(backend.signedInUserId).not.toBeNull();
  });

  it('shows the backend message for a wrong password without deleting anything', async () => {
    backend.signedInAs('thabo@example.com', 'Thabo');
    const { user } = renderApp(['/account']);
    await user.type(await screen.findByLabelText('Confirm your password'), 'WrongPassword1');
    await user.click(screen.getByRole('button', { name: 'Delete account' }));
    const dialog = await screen.findByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: 'Delete account' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Incorrect password');
    expect(backend.signedInUserId).not.toBeNull();
    expect(screen.getByRole('heading', { name: 'Account settings' })).toBeInTheDocument();
  });

  it('blocks the only admin, with the reason the backend gives', async () => {
    backend.signedInAs('admin@example.com', 'Admin', 'ADMIN');
    const { user } = renderApp(['/account']);
    await user.type(await screen.findByLabelText('Confirm your password'), 'Secret123');
    await user.click(screen.getByRole('button', { name: 'Delete account' }));
    const dialog = await screen.findByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: 'Delete account' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/only admin/i);
    expect(backend.signedInUserId).not.toBeNull();
  });

  it('is reachable from the account menu', async () => {
    backend.signedInAs('thabo@example.com', 'Thabo');
    const { user } = renderApp(['/']);
    await user.click(await screen.findByRole('button', { name: /account menu for thabo/i }));
    await user.click(screen.getByRole('link', { name: 'Account settings' }));
    expect(await screen.findByRole('heading', { name: 'Account settings' })).toBeInTheDocument();
    expect(screen.getAllByText('thabo@example.com').length).toBeGreaterThan(0);
  });

  it('has no detectable accessibility violations, including the confirmation', async () => {
    backend.signedInAs('thabo@example.com', 'Thabo');
    const { user, container } = renderApp(['/account']);
    await screen.findByRole('heading', { name: 'Account settings' });
    await expectNoAxeViolations(container);
    await user.type(screen.getByLabelText('Confirm your password'), 'Secret123');
    await user.click(screen.getByRole('button', { name: 'Delete account' }));
    await screen.findByRole('dialog');
    await expectNoAxeViolations(container);
  });
});

describe('Signing in again cancels a scheduled deletion', () => {
  it('shows a dismissible welcome-back banner after a successful sign-in', async () => {
    backend.addUser('thabo@example.com', 'Secret123', 'Thabo');
    backend.scheduleDeletion('thabo@example.com');
    const { user } = renderApp(['/login']);

    await user.type(await screen.findByLabelText('Email address'), 'thabo@example.com');
    await user.type(screen.getByLabelText('Password'), 'Secret123');
    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    await screen.findByRole('heading', { name: 'Welcome, Thabo' });
    const banner = await screen.findByText(/your account deletion was cancelled/i);
    expect(banner.closest('[role="status"]')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Dismiss this message' }));
    expect(screen.queryByText(/your account deletion was cancelled/i)).toBeNull();
  });

  it('does not show the banner, and does not cancel anything, on an ordinary sign-in', async () => {
    backend.addUser('thabo@example.com', 'Secret123', 'Thabo');
    const { user } = renderApp(['/login']);
    await user.type(await screen.findByLabelText('Email address'), 'thabo@example.com');
    await user.type(screen.getByLabelText('Password'), 'Secret123');
    await user.click(screen.getByRole('button', { name: 'Sign in' }));
    await screen.findByRole('heading', { name: 'Welcome, Thabo' });
    expect(screen.queryByText(/account deletion was cancelled/i)).toBeNull();
  });

  it('a wrong password leaves the scheduled deletion in place', async () => {
    backend.addUser('thabo@example.com', 'Secret123', 'Thabo');
    backend.scheduleDeletion('thabo@example.com');
    const { user } = renderApp(['/login']);
    await user.type(await screen.findByLabelText('Email address'), 'thabo@example.com');
    await user.type(screen.getByLabelText('Password'), 'WrongPassword1');
    await user.click(screen.getByRole('button', { name: 'Sign in' }));
    await screen.findByRole('alert');
    expect(screen.getByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
  });
});
