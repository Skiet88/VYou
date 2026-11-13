import { useEffect, useRef, useState } from 'react';
import { Link, NavLink, Outlet, useLocation } from 'react-router-dom';
import { useAccountReactivatedFlash, useCurrentUser } from '../../hooks/useAuth';
import { AccountMenu } from './AccountMenu';
import { DismissibleBanner } from './DismissibleBanner';
import { isInitialLocation } from './navigationFocus';
import styles from './AppShell.module.css';

interface NavItem {
  to: string;
  label: string;
  end?: boolean;
  adminOnly?: boolean;
}

/** Kept to a handful of top-level destinations; everything else is reached by drilling in. */
const NAV_ITEMS: NavItem[] = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/interviews', label: 'Interviews' },
  { to: '/users', label: 'Users', adminOnly: true },
];

export function AppShell() {
  const { data: user } = useCurrentUser();
  const accountReactivated = useAccountReactivatedFlash();
  const location = useLocation();
  // The small-screen menu remembers which page it was opened on, so it is closed again
  // automatically after any navigation, with no effect needed.
  const here = `${location.pathname}${location.search}`;
  const [navOpenOn, setNavOpenOn] = useState<string | null>(null);
  const navOpen = navOpenOn === here;
  const mainRef = useRef<HTMLElement>(null);

  // On client-side navigation (including arriving from sign-in/sign-up, which also mounts this
  // shell), move focus to the new page's heading so keyboard and screen-reader users start at the
  // new content. The page the browser loaded (or reloaded) is left alone: focus already starts at
  // the top of the document there.
  useEffect(() => {
    if (isInitialLocation(location.key)) return;
    const heading = mainRef.current?.querySelector('h1');
    const target = heading ?? mainRef.current;
    if (target) {
      target.setAttribute('tabindex', '-1');
      target.focus();
    }
  }, [location.key, location.pathname, location.search]);

  const items = NAV_ITEMS.filter((item) => !item.adminOnly || user?.role === 'ADMIN');

  return (
    <div className={styles.shell}>
      <a href="#main-content" className={styles.skipLink}>
        Skip to main content
      </a>
      <header className={styles.topbar}>
        <div className={styles.topbarStart}>
          <button
            type="button"
            className={styles.menuButton}
            aria-expanded={navOpen}
            aria-controls="primary-navigation"
            onClick={() => setNavOpenOn(navOpen ? null : here)}
          >
            Menu
          </button>
          <Link to="/" className={styles.brand}>
            VYou
          </Link>
        </div>
        <AccountMenu />
      </header>
      <div className={styles.body}>
        <nav id="primary-navigation" aria-label="Main" className={[styles.sidebar, navOpen ? styles.sidebarOpen : ''].join(' ')}>
          <ul className={styles.navList}>
            {items.map((item) => (
              <li key={item.to}>
                <NavLink to={item.to} end={item.end} className={({ isActive }) => [styles.navLink, isActive ? styles.active : ''].join(' ')}>
                  {item.label}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
        <main id="main-content" ref={mainRef} tabIndex={-1} className={styles.main}>
          {accountReactivated && <DismissibleBanner>Welcome back — your account deletion was cancelled.</DismissibleBanner>}
          <Outlet />
        </main>
      </div>
    </div>
  );
}
