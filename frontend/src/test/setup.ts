import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterAll, afterEach, beforeAll, beforeEach } from 'vitest';
import { backend, TEST_CSRF_TOKEN } from './msw/backend';
import { server } from './msw/server';

// Any request without a matching handler fails the test instead of silently hitting the network.
beforeAll(() => server.listen({ onUnhandledRequest: 'error' }));
afterEach(() => {
  cleanup();
  server.resetHandlers();
});
afterAll(() => server.close());

beforeEach(() => {
  backend.reset();
  // The real backend sets this cookie on any response; in jsdom we set it directly.
  document.cookie = `XSRF-TOKEN=${TEST_CSRF_TOKEN}; path=/`;
  document.documentElement.removeAttribute('data-theme');
  window.localStorage.clear();
});

// jsdom has no matchMedia; default to "light" unless a test overrides it.
if (!window.matchMedia) {
  window.matchMedia = (query: string) =>
    ({
      matches: false,
      media: query,
      onchange: null,
      addEventListener: () => {},
      removeEventListener: () => {},
      addListener: () => {},
      removeListener: () => {},
      dispatchEvent: () => false,
    }) as MediaQueryList;
}
