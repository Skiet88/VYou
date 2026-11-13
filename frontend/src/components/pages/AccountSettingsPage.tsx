import { useCurrentUser } from '../../hooks/useAuth';
import { DeleteAccountSection } from '../account/DeleteAccountSection';
import { PageLayout } from '../app/PageLayout';
import styles from './AccountSettingsPage.module.css';

export function AccountSettingsPage() {
  const { data: user } = useCurrentUser();
  return (
    <PageLayout title="Account settings" width="narrow">
      <h1>Account settings</h1>
      {user && (
        <dl className={styles.details}>
          <div>
            <dt>Name</dt>
            <dd>{user.displayName}</dd>
          </div>
          <div>
            <dt>Email</dt>
            <dd>{user.email}</dd>
          </div>
        </dl>
      )}
      <DeleteAccountSection />
    </PageLayout>
  );
}
