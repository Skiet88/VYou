import { Component, type ReactNode } from 'react';
import { ErrorState } from '../ui/StatusMessage';

/** Last line of defence: a render error shows a recovery screen instead of a blank page. */
export class AppErrorBoundary extends Component<{ children: ReactNode }, { hasError: boolean }> {
  state = { hasError: false };

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  // No componentDidCatch logging on purpose: error objects can contain user content.

  render() {
    if (this.state.hasError) {
      return <ErrorState message="Something went wrong. Reload the page to continue." onRetry={() => window.location.reload()} />;
    }
    return this.props.children;
  }
}
