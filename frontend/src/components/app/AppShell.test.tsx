import { screen, waitFor, within } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { backend } from '../../test/msw/backend';
import { expectNoAxeViolations } from '../../test/a11y';
import { renderApp } from '../../test/renderApp';

const mainNav = () => screen.getByRole('navigation', { name: 'Main' });

describe('App shell', () => {
  it('shows the main navigation with the current page marked', async () => {
    backend.signedInAs();
    renderApp(['/interviews']);
    await screen.findByRole('heading', { name: 'Interviews' });

    const nav = mainNav();
    expect(within(nav).getByRole('link', { name: 'Dashboard' })).not.toHaveAttribute('aria-current');
    expect(within(nav).getByRole('link', { name: 'Interviews' })).toHaveAttribute('aria-current', 'page');
  });

  it('marks New interview, and only that, as current while starting one', async () => {
    backend.signedInAs();
    renderApp(['/interviews/new']);
    await screen.findByRole('heading', {
      name: 'What role are you practising for?',
    });
    expect(within(mainNav()).getByRole('link', { name: 'New interview' })).toHaveAttribute('aria-current', 'page');
    expect(within(mainNav()).getByRole('link', { name: 'Interviews' })).not.toHaveAttribute('aria-current');
  });

  it('only shows Users to admins, and hides the page itself from everyone else', async () => {
    backend.signedInAs('user@example.com', 'User');
    const { unmount } = renderApp(['/users']);
    expect(await screen.findByRole('heading', { name: 'Page not found' })).toBeInTheDocument();
    unmount();

    backend.signedInAs('admin@example.com', 'Admin', 'ADMIN');
    renderApp(['/users']);
    expect(await screen.findByRole('heading', { name: 'Users' })).toBeInTheDocument();
    expect(within(mainNav()).getByRole('link', { name: 'Users' })).toBeInTheDocument();
  });

  it('hides Users from the navigation for non-admins', async () => {
    backend.signedInAs();
    renderApp(['/']);
    await screen.findByRole('heading', { name: 'Welcome, Thabo' });
    expect(within(mainNav()).queryByRole('link', { name: 'Users' })).toBeNull();
  });

  it('has a skip link to the main content', async () => {
    backend.signedInAs();
    renderApp(['/']);
    await screen.findByRole('heading', { name: 'Welcome, Thabo' });
    expect(screen.getByRole('link', { name: 'Skip to main content' })).toHaveAttribute('href', '#main-content');
    expect(document.getElementById('main-content')?.tagName).toBe('MAIN');
  });

  it('moves focus to the new page heading after navigating', async () => {
    backend.signedInAs();
    const { user } = renderApp(['/']);
    await screen.findByRole('heading', { name: 'Welcome, Thabo' });

    await user.click(within(mainNav()).getByRole('link', { name: 'Interviews' }));
    await waitFor(() => expect(screen.getByRole('heading', { level: 1, name: 'Interviews' })).toHaveFocus());
  });

  it('account menu opens, closes on Escape returning focus, and signs out', async () => {
    backend.signedInAs();
    const { user } = renderApp(['/']);
    const toggle = await screen.findByRole('button', {
      name: /account menu for thabo/i,
    });
    expect(toggle).toHaveAttribute('aria-expanded', 'false');

    await user.click(toggle);
    expect(toggle).toHaveAttribute('aria-expanded', 'true');
    expect(screen.getByText('thabo@example.com')).toBeVisible();

    await user.keyboard('{Escape}');
    expect(toggle).toHaveAttribute('aria-expanded', 'false');
    expect(toggle).toHaveFocus();

    await user.click(toggle);
    await user.click(screen.getByRole('button', { name: 'Sign out' }));
    expect(await screen.findByRole('heading', { name: 'Sign in' })).toBeInTheDocument();
    expect(backend.signedInUserId).toBeNull();
  });

  it('the small-screen menu button controls the navigation and closes after navigating', async () => {
    backend.signedInAs();
    const { user } = renderApp(['/']);
    const menu = await screen.findByRole('button', { name: 'Menu' });
    expect(menu).toHaveAttribute('aria-controls', 'primary-navigation');

    await user.click(menu);
    expect(menu).toHaveAttribute('aria-expanded', 'true');
    await user.click(within(mainNav()).getByRole('link', { name: 'Interviews' }));
    await screen.findByRole('heading', { name: 'Interviews' });
    expect(menu).toHaveAttribute('aria-expanded', 'false');
  });

  it('shows breadcrumbs on nested pages', async () => {
    backend.signedInAs();
    renderApp(['/interviews/new']);
    const crumbs = await screen.findByRole('navigation', {
      name: 'Breadcrumb',
    });
    expect(within(crumbs).getByRole('link', { name: 'Interviews' })).toHaveAttribute('href', '/interviews');
    expect(within(crumbs).getByText('New interview')).toHaveAttribute('aria-current', 'page');
  });

  it('has no detectable accessibility violations', async () => {
    backend.signedInAs('admin@example.com', 'Admin', 'ADMIN');
    const { container, user } = renderApp(['/']);
    await screen.findByRole('heading', { name: 'Welcome, Admin' });
    await expectNoAxeViolations(container);
    await user.click(screen.getByRole('button', { name: /account menu for admin/i }));
    await expectNoAxeViolations(container);
  });
});
