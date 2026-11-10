import type { ReactNode } from 'react';
import { useDocumentTitle } from '../../hooks/useDocumentTitle';
import { Breadcrumbs, type Crumb } from '../ui/Breadcrumbs';
import styles from './PageLayout.module.css';

interface PageLayoutProps {
  title: string;
  breadcrumbs?: Crumb[];
  width?: 'narrow' | 'wide';
  children: ReactNode;
}

/** Common page frame inside the app shell: document title, optional breadcrumbs, readable width. */
export function PageLayout({ title, breadcrumbs, width = 'wide', children }: PageLayoutProps) {
  useDocumentTitle(title);
  return (
    <div className={[styles.page, width === 'narrow' ? styles.narrow : ''].join(' ')}>
      {breadcrumbs && <Breadcrumbs items={breadcrumbs} />}
      {children}
    </div>
  );
}
