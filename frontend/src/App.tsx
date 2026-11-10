import { BrowserRouter } from 'react-router-dom';
import { AppProviders } from './AppProviders';
import { AppRoutes } from './components/app/AppRoutes';
import { createServices } from './services';
import { createQueryClient } from './state/queryClient';

const services = createServices();
const queryClient = createQueryClient();

export default function App() {
  return (
    <AppProviders services={services} queryClient={queryClient}>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </AppProviders>
  );
}
