import { Route, Routes } from 'react-router-dom';
import { RedirectIfAuthenticated, RequireAdmin, RequireAuth } from '../auth/RouteGuards';
import { AccountSettingsPage } from '../pages/AccountSettingsPage';
import { DashboardPage } from '../pages/DashboardPage';
import { InterviewPage } from '../pages/InterviewPage';
import { InterviewsPage } from '../pages/InterviewsPage';
import { LoginPage } from '../pages/LoginPage';
import { NewInterviewPage } from '../pages/NewInterviewPage';
import { NotFoundPage } from '../pages/NotFoundPage';
import { SignupPage } from '../pages/SignupPage';
import { UsersPage } from '../pages/UsersPage';
import { AppShell } from './AppShell';
import { useRecordInitialLocation } from './navigationFocus';

export function AppRoutes() {
  useRecordInitialLocation();
  return (
    <Routes>
      <Route
        element={
          <RequireAuth>
            <AppShell />
          </RequireAuth>
        }
      >
        <Route index element={<DashboardPage />} />
        <Route path="account" element={<AccountSettingsPage />} />
        <Route path="interviews" element={<InterviewsPage />} />
        <Route path="interviews/new" element={<NewInterviewPage />} />
        <Route path="interviews/:id" element={<InterviewPage />} />
        <Route
          path="users"
          element={
            <RequireAdmin>
              <UsersPage />
            </RequireAdmin>
          }
        />
      </Route>
      <Route path="/login" element={<RedirectIfAuthenticated><LoginPage /></RedirectIfAuthenticated>} />
      <Route path="/signup" element={<RedirectIfAuthenticated><SignupPage /></RedirectIfAuthenticated>} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  );
}
