import type { ReactNode } from 'react';
import { Icon } from '../ui/Icon';
import { ThemeToggle } from '../ui/ThemeToggle';
import styles from './AuthLayout.module.css';

interface AuthLayoutProps {
  title: string;
  intro?: string;
  children: ReactNode;
  footer: ReactNode;
}

export function AuthLayout({ title, intro, children, footer }: AuthLayoutProps) {
  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <span className={styles.brand}>
          <span className={styles.logo}>
            <Icon name="brain" />
          </span>
          VYou
        </span>
        <ThemeToggle />
      </header>
      <main className={styles.main}>
        <div className={styles.panel}>
          <h1>{title}</h1>
          {intro && <p className={styles.intro}>{intro}</p>}
          {children}
          <p className={styles.footer}>{footer}</p>
        </div>
      </main>
    </div>
  );
}
