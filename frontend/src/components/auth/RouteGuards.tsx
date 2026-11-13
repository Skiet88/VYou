import type { ReactNode } from 'react';
import { NotFoundPage } from '../pages/NotFoundPage';
import { Navigate, useLocation } from 'react-router-dom';
import { useCurrentUser } from '../../hooks/useAuth';
import { ErrorState, LoadingState } from '../ui/StatusMessage';

/**
 * Signed-out visitors go to sign in, then come back to where they were heading. A signed-in but
 * unverified account is sent to the pending-verification page instead of the requested content;
 * the backend blocks every API call the same way, so this only saves a round trip.
 */
export function RequireAuth({ children }: { children: ReactNode }) {
  const location = useLocation();
  const currentUser = useCurrentUser();

  if (currentUser.isPending) return <LoadingState />;
  if (currentUser.isError) {
    return <ErrorState message="Could not reach VYou. Check your connection and try again." onRetry={() => currentUser.refetch()} />;
  }
  if (currentUser.data === null) {
    return <Navigate to="/login" replace state={{ from: `${location.pathname}${location.search}${location.hash}` }} />;
  }
  if (!currentUser.data.emailVerified) {
    return <Navigate to="/verify-email/pending" replace />;
  }
  return <>{children}</>;
}

/** The pending-verification page is only useful while unverified; otherwise go to the app. */
export function RequireUnverified({ children }: { children: ReactNode }) {
  const currentUser = useCurrentUser();
  if (currentUser.isPending) return <LoadingState />;
  if (currentUser.isError) {
    return <ErrorState message="Could not reach VYou. Check your connection and try again." onRetry={() => currentUser.refetch()} />;
  }
  if (!currentUser.data) return <Navigate to="/login" replace />;
  if (currentUser.data.emailVerified) return <Navigate to="/" replace />;
  return <>{children}</>;
}

/** Sign-in and sign-up pages are pointless when already signed in. */
export function RedirectIfAuthenticated({ children }: { children: ReactNode }) {
  const currentUser = useCurrentUser();
  if (currentUser.isPending) return <LoadingState />;
  if (currentUser.data) return <Navigate to="/" replace />;
  // On a network error, still show the form: submitting it will report the problem clearly.
  return <>{children}</>;
}

/**
 * Admin-only pages. This only hides UI: every admin capability must also be enforced by the
 * backend, which decides roles from its own database. Non-admins see "not found" rather than
 * "forbidden", so the page's existence isn't advertised.
 */
export function RequireAdmin({ children }: { children: ReactNode }) {
  const currentUser = useCurrentUser();
  if (currentUser.isPending) return <LoadingState />;
  if (currentUser.data?.role !== 'ADMIN') return <NotFoundPage />;
  return <>{children}</>;
}
