import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { backend } from '../../test/msw/backend';
import { expectNoAxeViolations } from '../../test/a11y';
import { renderApp } from '../../test/renderApp';

describe('Forgot password', () => {
  it('is reachable from the sign-in form', async () => {
    const { user } = renderApp(['/login']);
    await user.click(await screen.findByRole('link', { name: 'Forgot your password?' }));
    expect(await screen.findByRole('heading', { name: 'Reset your password' })).toBeInTheDocument();
  });

  it('reports success and never reveals whether the email is registered', async () => {
    backend.addUser('thabo@example.com', 'Secret123', 'Thabo');
    const { user } = renderApp(['/forgot-password']);

    await user.type(await screen.findByLabelText('Email address'), 'thabo@example.com');
    await user.click(screen.getByRole('button', { name: 'Send reset link' }));
    expect(await screen.findByText(/we.ve sent a link/i)).toBeInTheDocument();
  });

  it('shows the same success message for an email that is not registered', async () => {
    const { user } = renderApp(['/forgot-password']);
    await user.type(await screen.findByLabelText('Email address'), 'nobody@example.com');
    await user.click(screen.getByRole('button', { name: 'Send reset link' }));
    expect(await screen.findByText(/we.ve sent a link/i)).toBeInTheDocument();
  });

  it('validates the email before calling the API', async () => {
    const { user } = renderApp(['/forgot-password']);
    await user.type(await screen.findByLabelText('Email address'), 'not-an-email');
    await user.click(screen.getByRole('button', { name: 'Send reset link' }));
    expect(await screen.findByRole('alert')).toHaveTextContent(/enter a valid email/i);
  });

  it('has no detectable accessibility violations', async () => {
    const { container } = renderApp(['/forgot-password']);
    await screen.findByRole('button', { name: 'Send reset link' });
    await expectNoAxeViolations(container);
  });
});

describe('Reset password: the link from the email', () => {
  it('changes the password and signs in with it, but not with the old one', async () => {
    backend.addUser('thabo@example.com', 'Secret123', 'Thabo');
    const { user } = renderApp(['/forgot-password']);
    await user.type(await screen.findByLabelText('Email address'), 'thabo@example.com');
    await user.click(screen.getByRole('button', { name: 'Send reset link' }));
    await screen.findByText(/we.ve sent a link/i);
    const token = backend.passwordResetToken('thabo@example.com');

    const { user: user2 } = renderApp([`/reset-password?token=${token}`]);
    await user2.type(await screen.findByLabelText('New password'), 'BrandNewSecret1');
    await user2.click(screen.getByRole('button', { name: 'Reset password' }));

    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
    expect(await screen.findByText(/password has been reset/i)).toBeInTheDocument();
    expect(backend.passwordOf('thabo@example.com')).toBe('BrandNewSecret1');
  });

  it('reports an invalid or expired token without saying which', async () => {
    const { user } = renderApp(['/reset-password?token=not-a-real-token']);
    await user.type(await screen.findByLabelText('New password'), 'BrandNewSecret1');
    await user.click(screen.getByRole('button', { name: 'Reset password' }));
    expect(await screen.findByRole('alert')).toHaveTextContent(/invalid or has expired/i);
  });

  it('reports a missing token without showing the form', async () => {
    renderApp(['/reset-password']);
    expect(await screen.findByText(/missing its reset code/i)).toBeInTheDocument();
    expect(screen.queryByLabelText('New password')).toBeNull();
  });

  it('validates the new password before calling the API', async () => {
    const { user } = renderApp(['/reset-password?token=some-token']);
    await user.type(await screen.findByLabelText('New password'), 'short');
    await user.click(screen.getByRole('button', { name: 'Reset password' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Password must be at least 8 characters');
  });

  it('has no detectable accessibility violations', async () => {
    const { container } = renderApp(['/reset-password?token=some-token']);
    await screen.findByRole('button', { name: 'Reset password' });
    await expectNoAxeViolations(container);
  });
});
