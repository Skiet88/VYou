import styles from './ScoreList.module.css';

export interface ScoreItem {
  label: string;
  value: number;
}

/** Scores read as "7 out of 10" to screen readers, shown compactly as "7/10". */
export function ScoreValue({ value, decimals = 0 }: { value: number; decimals?: number }) {
  const text = value.toFixed(decimals);
  return (
    <>
      {text}
      <span aria-hidden="true" className={styles.outOf}>
        /10
      </span>
      <span className="visually-hidden"> out of 10</span>
    </>
  );
}

export function ScoreList({ items, decimals = 0 }: { items: ScoreItem[]; decimals?: number }) {
  return (
    <dl className={styles.list}>
      {items.map((item) => (
        <div key={item.label} className={styles.row}>
          <dt>{item.label}</dt>
          <dd className={styles.value}>
            <ScoreValue value={item.value} decimals={decimals} />
          </dd>
        </div>
      ))}
    </dl>
  );
}
