export function StatusBadge({ state }) {
  const normalised = (state || 'unknown').toLowerCase();
  const tone = normalised === 'running' ? 'ok' : normalised === 'stopped' ? 'muted' : 'warn';
  return <span className={`badge badge-${tone}`}>{normalised}</span>;
}
