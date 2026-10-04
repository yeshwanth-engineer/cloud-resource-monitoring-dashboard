import { formatPercent } from '../lib/format';

/** Four headline numbers: inventory size, uptime mix, and the CPU of the selected instance. */
export function SummaryStats({ instances, points, source, selectedName }) {
  const running = instances.filter((instance) => instance.state === 'running').length;
  const stopped = instances.length - running;
  const values = points.map((point) => point.value);
  const latest = values.length ? values[values.length - 1] : null;
  const peak = values.length ? Math.max(...values) : null;

  const cards = [
    { label: 'Instances', value: instances.length || '—', detail: 'in the selected region' },
    {
      label: 'Running',
      value: running,
      detail: `${stopped} stopped or stopping`,
      tone: running > 0 ? 'ok' : 'warn',
    },
    {
      label: 'Latest CPU',
      value: formatPercent(latest),
      detail: selectedName ? `peak ${formatPercent(peak)} · ${selectedName}` : 'select an instance',
    },
    {
      label: 'Data source',
      value: source === 'AWS' ? 'CloudWatch' : source === 'DEMO' ? 'Generated' : '—',
      detail: points.length ? `${points.length} points in window` : 'no datapoints in window',
    },
  ];

  return (
    <section className="stats" aria-label="Summary">
      {cards.map((card) => (
        <article className="card stat" key={card.label}>
          <p className="stat-label">{card.label}</p>
          <p className={`stat-value${card.tone ? ` stat-${card.tone}` : ''}`}>{card.value}</p>
          <p className="stat-detail">{card.detail}</p>
        </article>
      ))}
    </section>
  );
}
