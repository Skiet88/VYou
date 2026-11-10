import { screen, waitFor, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { backend, RATE_LIMITED_EMAIL } from '../../test/msw/backend';
import { expectNoAxeViolations } from '../../test/a11y';
import { renderApp } from '../../test/renderApp';

const location = () => screen.getByTestId('location').textContent;

describe('Login flow', () => {
  it('signs in and lands on the dashboard', async () => {
    backend.addUser('thabo@example.com', 'Secret123', 'Thabo');
    const { user } = renderApp(['/login']);

    await user.type(await screen.findByLabelText('Email address'), 'thabo@example.com');
    await user.type(screen.getByLabelText('Password'), 'Secret123');
    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByRole('heading', { name: 'Welcome, Thabo' })).toBeInTheDocument();
    expect(location()).toBe('/');
  });

  it('shows field errors, announces them, and moves focus to the summary without calling the API', async () => {
    const { user } = renderApp(['/login']);
    await user.click(await screen.findByRole('button', { name: 'Sign in' }));

    const summary = await screen.findByRole('alert');
    expect(within(summary).getByRole('link', { name: 'Enter your email address' })).toHaveAttribute('href', '#email');
    expect(within(summary).getByRole('link', { name: 'Enter your password' })).toBeInTheDocument();
    await waitFor(() => expect(summary).toHaveFocus());

    const email = screen.getByLabelText('Email address');
    expect(email).toHaveAttribute('aria-invalid', 'true');
    expect(email).toHaveAccessibleDescription(/enter your email address/i);
    expect(backend.lastRequestHeaders).toBeNull();
  });

  it('shows the generic server message for wrong credentials', async () => {
    backend.addUser('thabo@example.com', 'Secret123');
    const { user } = renderApp(['/login']);

    await user.type(await screen.findByLabelText('Email address'), 'thabo@example.com');
    await user.type(screen.getByLabelText('Password'), 'WrongPassword');
    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password');
    expect(location()).toBe('/login');
  });

  it('explains rate limiting', async () => {
    const { user } = renderApp(['/login']);
    await user.type(await screen.findByLabelText('Email address'), RATE_LIMITED_EMAIL);
    await user.type(screen.getByLabelText('Password'), 'Secret123');
    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/too many attempts/i);
  });

  it('returns to the page the user was heading for, but never off-site', async () => {
    backend.addUser('thabo@example.com', 'Secret123');
    const { user } = renderApp([{ pathname: '/login', state: { from: '//evil.example.com' } }]);

    await user.type(await screen.findByLabelText('Email address'), 'thabo@example.com');
    await user.type(screen.getByLabelText('Password'), 'Secret123');
    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    await waitFor(() => expect(location()).toBe('/'));
  });

  it('renders user-controlled text as text, never as HTML', async () => {
    backend.addUser('xss@example.com', 'Secret123', '<img src=x onerror=alert(1)>');
    const { user, container } = renderApp(['/login']);

    await user.type(await screen.findByLabelText('Email address'), 'xss@example.com');
    await user.type(screen.getByLabelText('Password'), 'Secret123');
    await user.click(screen.getByRole('button', { name: 'Sign in' }));

    expect(await screen.findByRole('heading', { name: 'Welcome, <img src=x onerror=alert(1)>' })).toBeInTheDocument();
    expect(container.querySelector('img')).toBeNull();
  });

  it('has no detectable accessibility violations, including with errors shown', async () => {
    const { user, container } = renderApp(['/login']);
    await screen.findByRole('button', { name: 'Sign in' });
    await expectNoAxeViolations(container);

    await user.click(screen.getByRole('button', { name: 'Sign in' }));
    await screen.findByRole('alert');
    await expectNoAxeViolations(container);
  });
});
