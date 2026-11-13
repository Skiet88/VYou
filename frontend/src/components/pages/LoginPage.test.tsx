import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { expectNoAxeViolations } from '../../test/a11y';
import { renderApp } from '../../test/renderApp';

describe('Arriving at sign-in after deleting an account', () => {
  it('shows a dismissible notice explaining the grace period', async () => {
    const { user } = renderApp([{ pathname: '/login', state: { accountDeleted: true } }]);
    await screen.findByRole('heading', { name: 'Sign in' });
    const banner = await screen.findByRole('status');
    expect(banner).toHaveTextContent('deleted in 30 days');
    await user.click(screen.getByRole('button', { name: 'Dismiss this message' }));
    expect(screen.queryByRole('status')).toBeNull();
  });

  it('shows nothing extra on an ordinary visit to sign in', async () => {
    renderApp(['/login']);
    await screen.findByRole('heading', { name: 'Sign in' });
    expect(screen.queryByRole('status')).toBeNull();
  });

  it('has no detectable accessibility violations with the notice shown', async () => {
    const { container } = renderApp([{ pathname: '/login', state: { accountDeleted: true } }]);
    await screen.findByRole('heading', { name: 'Sign in' });
    await screen.findByRole('status');
    await expectNoAxeViolations(container);
  });
});
