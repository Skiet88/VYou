import { describe, expect, it } from 'vitest';
import { backend } from '../test/msw/backend';
import { createServices } from '.';

describe('authService (against the mocked backend contract)', () => {
  it('currentUser is null without a session, not an error', async () => {
    await expect(createServices().auth.currentUser()).resolves.toBeNull();
  });

  it('currentUser returns the signed-in user', async () => {
    backend.addUser('thabo@example.com', 'Secret123', 'Thabo');
    backend.signIn('thabo@example.com');
    await expect(createServices().auth.currentUser()).resolves.toMatchObject({ email: 'thabo@example.com', displayName: 'Thabo' });
  });

  it('login sends the CSRF token and starts a session', async () => {
    backend.addUser('thabo@example.com', 'Secret123');
    await createServices().auth.login({ email: 'thabo@example.com', password: 'Secret123' });
    expect(backend.lastRequestHeaders?.get('X-XSRF-TOKEN')).toBe('test-csrf-token');
    expect(backend.signedInUserId).not.toBeNull();
  });

  it('logout ends the session', async () => {
    backend.addUser('thabo@example.com', 'Secret123');
    backend.signIn('thabo@example.com');
    await createServices().auth.logout();
    expect(backend.signedInUserId).toBeNull();
  });
});
