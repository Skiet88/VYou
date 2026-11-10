const dateFormat = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' });
const dateTimeFormat = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' });

/** Formats an ISO timestamp in the viewer's locale; returns '' for anything unparseable. */
export function formatDate(iso: string | null | undefined, withTime = false): string {
  if (!iso) return '';
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return '';
  return (withTime ? dateTimeFormat : dateFormat).format(date);
}
