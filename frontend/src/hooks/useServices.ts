import { useContext } from 'react';
import type { Services } from '../services';
import { ServicesContext } from '../state/services/ServicesContext';

export function useServices(): Services {
  const services = useContext(ServicesContext);
  if (services === null) throw new Error('useServices must be used inside <ServicesProvider>');
  return services;
}
