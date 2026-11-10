import { Link, type LinkProps } from 'react-router-dom';
import styles from './Button.module.css';

/** Navigation that should look like a button (e.g. "Start interview") but is still a real link. */
export function ButtonLink({ variant = 'secondary', className, ...rest }: LinkProps & { variant?: 'primary' | 'secondary' }) {
  return <Link {...rest} className={[styles.button, styles[variant], styles.link, className].filter(Boolean).join(' ')} />;
}
