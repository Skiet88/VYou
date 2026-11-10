import { screen, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { backend } from '../../test/msw/backend';
import { expectNoAxeViolations } from '../../test/a11y';
import { renderApp } from '../../test/renderApp';

describe('Sign-up flow', () => {
  it('creates an account, signs the user in and lands on the dashboard', async () => {
    const { user } = renderApp(['/signup']);

    await user.type(await screen.findByLabelText('Your name'), 'Lerato');
    await user.type(screen.getByLabelText('Email address'), 'lerato@example.com');
    await user.type(screen.getByLabelText(/create a password/i), 'Secret123');
    await user.click(screen.getByRole('button', { name: 'Create account' }));

    expect(await screen.findByRole('heading', { name: 'Welcome, Lerato' })).toBeInTheDocument();
    expect(backend.signedInUserId).not.toBeNull();
  });

  it('validates every field before calling the API', async () => {
    const { user } = renderApp(['/signup']);
    await user.type(await screen.findByLabelText('Email address'), 'not-an-email');
    await user.type(screen.getByLabelText(/create a password/i), 'short');
    await user.click(screen.getByRole('button', { name: 'Create account' }));

    const summary = await screen.findByRole('alert');
    expect(within(summary).getByText('Enter your name')).toBeInTheDocument();
    expect(within(summary).getByText(/enter a valid email address/i)).toBeInTheDocument();
    expect(within(summary).getByText('Password must be at least 8 characters')).toBeInTheDocument();
    expect(backend.lastRequestHeaders).toBeNull();
  });

  it('clears a field error as soon as the value becomes valid', async () => {
    const { user } = renderApp(['/signup']);
    await user.click(await screen.findByRole('button', { name: 'Create account' }));
    await screen.findByRole('alert');

    await user.type(screen.getByLabelText('Your name'), 'Lerato');
    expect(screen.getByLabelText('Your name')).not.toHaveAttribute('aria-invalid');
  });

  it('does not reveal whether an email is already registered', async () => {
    backend.addUser('taken@example.com', 'Secret123');
    const { user } = renderApp(['/signup']);

    await user.type(await screen.findByLabelText('Your name'), 'Someone');
    await user.type(screen.getByLabelText('Email address'), 'taken@example.com');
    await user.type(screen.getByLabelText(/create a password/i), 'Secret123');
    await user.click(screen.getByRole('button', { name: 'Create account' }));

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Could not create an account with the details provided');
    expect(alert).not.toHaveTextContent(/already|exists|registered/i);
  });

  it('links to sign in instead', async () => {
    const { user } = renderApp(['/signup']);
    await user.click(await screen.findByRole('link', { name: 'Sign in instead' }));
    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
  });

  it('has no detectable accessibility violations', async () => {
    const { container } = renderApp(['/signup']);
    await screen.findByRole('button', { name: 'Create account' });
    await expectNoAxeViolations(container);
  });
});
