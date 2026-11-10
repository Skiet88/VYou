import { QueryClientProvider, type QueryClient } from '@tanstack/react-query';
import type { ReactNode } from 'react';
import type { Services } from './services';
import { ServicesProvider } from './state/services/ServicesContext';
import { ThemeProvider } from './state/theme/ThemeContext';
import { AppErrorBoundary } from './components/app/AppErrorBoundary';

interface AppProvidersProps {
  services: Services;
  queryClient: QueryClient;
  children: ReactNode;
}

/**
 * Composition root: the one place that wires concrete state providers and services into the app.
 * Lives outside components/ on purpose. The router is supplied by the caller (Browser vs Memory).
 */
export function AppProviders({ services, queryClient, children }: AppProvidersProps) {
  return (
    <AppErrorBoundary>
      <ServicesProvider services={services}>
        <QueryClientProvider client={queryClient}>
          <ThemeProvider>{children}</ThemeProvider>
        </QueryClientProvider>
      </ServicesProvider>
    </AppErrorBoundary>
  );
}
