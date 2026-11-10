import { createContext, type ReactNode } from 'react';
import type { Services } from '../../services';

/**
 * Dependency injection for the services layer: hooks depend on the Services interface, never on
 * concrete implementations, so tests can supply fakes and the wiring lives in one place.
 */
export const ServicesContext = createContext<Services | null>(null);

export function ServicesProvider({ services, children }: { services: Services; children: ReactNode }) {
  return <ServicesContext.Provider value={services}>{children}</ServicesContext.Provider>;
}
