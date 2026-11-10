import { screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { backend } from '../../test/msw/backend';
import { server } from '../../test/msw/server';
import { renderApp } from '../../test/renderApp';

const location = () => screen.getByTestId('location').textContent;

describe('Route guards', () => {
  it('sends a signed-out visitor to sign in, then back to the page they wanted', async () => {
    backend.addUser('thabo@example.com', 'Secret123', 'Thabo');
    const { user } = renderApp(['/?tab=recent']);

    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
    expect(location()).toBe('/login');

    await user.type(screen.getByLabelText('Email address'), 'thabo@example.com');
    await user.type(screen.getByLabelText('Password'), 'Secret123');
    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByRole('heading', { name: 'Welcome, Thabo' })).toBeInTheDocument();
  });

  it('skips the sign-in page for someone already signed in', async () => {
    backend.addUser('thabo@example.com', 'Secret123', 'Thabo');
    backend.signIn('thabo@example.com');
    renderApp(['/login']);

    expect(await screen.findByRole('heading', { name: 'Welcome, Thabo' })).toBeInTheDocument();
    expect(location()).toBe('/');
  });

  it('signing out ends the session and returns to sign in', async () => {
    backend.addUser('thabo@example.com', 'Secret123', 'Thabo');
    backend.signIn('thabo@example.com');
    const { user } = renderApp(['/']);

    await user.click(await screen.findByRole('button', { name: /account menu for thabo/i }));
    await user.click(screen.getByRole('button', { name: 'Sign out' }));

    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
    expect(backend.signedInUserId).toBeNull();
  });

  it('shows a retryable error state when the backend is unreachable', async () => {
    server.use(http.get('*/api/auth/me', () => HttpResponse.error()));
    renderApp(['/']);

    expect(await screen.findByRole('alert')).toHaveTextContent(/could not reach vyou/i);
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument();
  });

  it('shows a not-found page for unknown addresses', async () => {
    renderApp(['/does-not-exist']);
    expect(await screen.findByRole('heading', { name: 'Page not found' })).toBeInTheDocument();
    await waitFor(() => expect(document.title).toBe('Page not found · VYou'));
  });
});

describe('Focus after signing in', () => {
  it('lands on the dashboard heading, not mid-page where the form used to be', async () => {
    backend.addUser('thabo@example.com', 'Secret123', 'Thabo');
    const { user } = renderApp(['/login']);
    await user.type(await screen.findByLabelText('Email address'), 'thabo@example.com');
    await user.type(screen.getByLabelText('Password'), 'Secret123');
    await user.click(screen.getByRole('button', { name: 'Sign in' }));
    const heading = await screen.findByRole('heading', { name: 'Welcome, Thabo' });
    await waitFor(() => expect(heading).toHaveFocus());
  });
});
