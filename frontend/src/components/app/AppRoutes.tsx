import { Route, Routes } from 'react-router-dom';
import { RedirectIfAuthenticated, RequireAuth } from '../auth/RouteGuards';
import { DashboardPage } from '../pages/DashboardPage';
import { LoginPage } from '../pages/LoginPage';
import { NotFoundPage } from '../pages/NotFoundPage';
import { SignupPage } from '../pages/SignupPage';

export function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<RequireAuth><DashboardPage /></RequireAuth>} />
      <Route path="/login" element={<RedirectIfAuthenticated><LoginPage /></RedirectIfAuthenticated>} />
      <Route path="/signup" element={<RedirectIfAuthenticated><SignupPage /></RedirectIfAuthenticated>} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
