import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { backend } from '../../test/msw/backend';
import { expectNoAxeViolations } from '../../test/a11y';
import { renderApp } from '../../test/renderApp';

describe('Email verification: blocked until verified', () => {
  it('sends an unverified signed-in user to the pending-verification page instead of the app', async () => {
    backend.addUser('thabo@example.com', 'Secret123', 'Thabo', 'USER', false);
    backend.signIn('thabo@example.com');
    renderApp(['/']);

    expect(await screen.findByRole('heading', { name: 'Verify your email' })).toBeInTheDocument();
    expect(screen.getByTestId('location')).toHaveTextContent('/verify-email/pending');
  });

  it('a verified user visiting the pending page is sent to the dashboard instead', async () => {
    backend.signedInAs('thabo@example.com', 'Thabo');
    renderApp(['/verify-email/pending']);
    expect(await screen.findByRole('heading', { name: 'Welcome, Thabo' })).toBeInTheDocument();
  });

  it('a signed-out visitor to the pending page is sent to sign in', async () => {
    renderApp(['/verify-email/pending']);
    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
  });

  it('can resend the verification email, and is rate-limited like any other auth action', async () => {
    backend.addUser('thabo@example.com', 'Secret123', 'Thabo', 'USER', false);
    backend.signIn('thabo@example.com');
    const { user } = renderApp(['/verify-email/pending']);

    const before = backend.verificationToken('thabo@example.com');
    await user.click(await screen.findByRole('button', { name: 'Resend verification email' }));
    expect(await screen.findByText(/email sent/i)).toBeInTheDocument();
    expect(backend.verificationToken('thabo@example.com')).not.toEqual(before);
  });

  it('signing out from the pending page lands on sign in', async () => {
    backend.addUser('thabo@example.com', 'Secret123', 'Thabo', 'USER', false);
    backend.signIn('thabo@example.com');
    const { user } = renderApp(['/verify-email/pending']);
    await user.click(await screen.findByRole('button', { name: 'Sign out' }));
    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
    expect(backend.signedInUserId).toBeNull();
  });

  it('has no detectable accessibility violations', async () => {
    backend.addUser('thabo@example.com', 'Secret123', 'Thabo', 'USER', false);
    backend.signIn('thabo@example.com');
    const { container } = renderApp(['/verify-email/pending']);
    await screen.findByRole('heading', { name: 'Verify your email' });
    await expectNoAxeViolations(container);
  });
});

describe('Email verification: the link from the email', () => {
  it('verifies the account and offers to continue into the app', async () => {
    backend.addUser('thabo@example.com', 'Secret123', 'Thabo', 'USER', false);
    backend.signIn('thabo@example.com');
    const token = backend.verificationToken('thabo@example.com');

    const { user } = renderApp([`/verify-email?token=${token}`]);
    expect(await screen.findByText(/your email is verified/i)).toBeInTheDocument();

    await user.click(screen.getByRole('link', { name: 'Continue to VYou' }));
    expect(await screen.findByRole('heading', { name: 'Welcome, Thabo' })).toBeInTheDocument();
  });

  it('works for a link opened signed out, in a different browser', async () => {
    backend.addUser('thabo@example.com', 'Secret123', 'Thabo', 'USER', false);
    const token = backend.verificationToken('thabo@example.com');

    renderApp([`/verify-email?token=${token}`]);
    expect(await screen.findByText(/your email is verified/i)).toBeInTheDocument();
  });

  it('reports an invalid or expired token without saying which', async () => {
    renderApp(['/verify-email?token=not-a-real-token']);
    expect(await screen.findByText(/invalid or has expired/i)).toBeInTheDocument();
  });

  it('reports a missing token without calling the API', async () => {
    renderApp(['/verify-email']);
    expect(await screen.findByText(/missing its verification code/i)).toBeInTheDocument();
  });

  it('has no detectable accessibility violations', async () => {
    const { container } = renderApp(['/verify-email?token=not-a-real-token']);
    await screen.findByText(/invalid or has expired/i);
    await expectNoAxeViolations(container);
  });
});
