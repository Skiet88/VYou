import { useTheme } from '../../hooks/useTheme';
import { Button } from './Button';

/** Text, not an icon: the label says exactly what pressing it will do. */
export function ThemeToggle() {
  const { theme, toggleTheme } = useTheme();
  return <Button onClick={toggleTheme}>{theme === 'dark' ? 'Light theme' : 'Dark theme'}</Button>;
}
