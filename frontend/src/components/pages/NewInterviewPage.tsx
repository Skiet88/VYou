import { PageLayout } from '../app/PageLayout';
import { NewInterviewWizard } from '../interview/NewInterviewWizard';

export function NewInterviewPage() {
  return (
    <PageLayout
      title="New interview"
      width="narrow"
      breadcrumbs={[{ label: 'Interviews', to: '/interviews' }, { label: 'New interview' }]}
    >
      <NewInterviewWizard />
    </PageLayout>
  );
}
