import { useEffect, useState } from 'react';

/**
 * Dev-loop connectivity check only - not a real screen. It proves the Vite proxy, the
 * session cookie and the backend are wired together correctly, before any actual UI
 * (components/hooks/services/schemas layers) exists.
 *
 * A 401 here is a SUCCESS: it means the request reached the backend through the proxy and
 * got a real, well-formed response. Failure to reach the backend at all shows as a network
 * error instead, which is the thing this check is actually watching for.
 */
type ConnectivityState =
  | { status: 'loading' }
  | { status: 'reachable'; httpStatus: number }
  | { status: 'unreachable'; message: string };

function useBackendConnectivity(): ConnectivityState {
  const [state, setState] = useState<ConnectivityState>({ status: 'loading' });

  useEffect(() => {
    let cancelled = false;

    fetch('/api/auth/me', { credentials: 'include' })
      .then((response) => {
        if (!cancelled) {
          setState({ status: 'reachable', httpStatus: response.status });
        }
      })
      .catch((error: unknown) => {
        if (!cancelled) {
          setState({
            status: 'unreachable',
            message: error instanceof Error ? error.message : 'Unknown network error',
          });
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  return state;
}

export default function App() {
  const connectivity = useBackendConnectivity();

  return (
    <main style={{ fontFamily: 'system-ui, sans-serif', padding: '2rem', maxWidth: 640 }}>
      <h1>VYou2 dev loop</h1>
      <p>
        This page only checks that the frontend can reach the backend through the Vite proxy.
        The real application (login, dashboard, interviews) is built on top of this once the
        loop is confirmed working.
      </p>
      {connectivity.status === 'loading' && <p>Checking backend connectivity…</p>}
      {connectivity.status === 'reachable' && (
        <p>
          Backend reachable through <code>/api</code> proxy — received HTTP{' '}
          {connectivity.httpStatus} from <code>/api/auth/me</code>
          {connectivity.httpStatus === 401 ? ' (expected: not logged in).' : '.'}
        </p>
      )}
      {connectivity.status === 'unreachable' && (
        <p role="alert">
          Could not reach the backend: {connectivity.message}. Is it running on port 8080?
        </p>
      )}
    </main>
  );
}
