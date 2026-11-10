import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { AppProviders } from '../AppProviders';
import { AppRoutes } from '../components/app/AppRoutes';
import { createServices } from '../services';
import { createQueryClient } from '../state/queryClient';

/** Shows the current route, so tests can assert where navigation ended up. */
function LocationProbe() {
  const location = useLocation();
  return <output data-testid="location">{location.pathname}</output>;
}

/**
 * Renders the real app (real services, real hooks, real routes) against the MSW backend.
 * Only the router is swapped for an in-memory one.
 */
export function renderApp(initialEntries: Array<string | { pathname: string; state?: unknown }> = ['/']) {
  const user = userEvent.setup();
  const queryClient = createQueryClient({ retryDelayMs: 0 });
  const utils = render(
    <AppProviders services={createServices()} queryClient={queryClient}>
      <MemoryRouter initialEntries={initialEntries}>
        <AppRoutes />
        <Routes>
          <Route path="*" element={<LocationProbe />} />
        </Routes>
      </MemoryRouter>
    </AppProviders>,
  );
  return { user, queryClient, ...utils };
}
