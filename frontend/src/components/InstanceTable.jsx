import { formatAge, formatDateTime } from '../lib/format';
import { StatusBadge } from './StatusBadge';

const COLUMNS = ['Instance', 'Type', 'State', 'Private IP', 'Public IP', 'Launched'];

/**
 * Clickable EC2 table. The selected row drives the CPU chart, so rows are keyboard reachable too.
 */
export function InstanceTable({ instances, selectedId, loading, error, onSelect }) {
  function handleRowKeyDown(event, instanceId) {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      onSelect(instanceId);
    }
  }

  return (
    <section className="card panel" aria-label="EC2 instances">
      <div className="panel-head">
        <div>
          <h2>EC2 instances</h2>
          <p className="muted">Select a row to load its CPU history.</p>
        </div>
        <span className="count">{instances.length} total</span>
      </div>

      {error && <p className="panel-note panel-note-error">{error}</p>}
      {loading && !instances.length ? (
        <p className="panel-note">Loading inventory…</p>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                {COLUMNS.map((column) => (
                  <th key={column} scope="col">
                    {column}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {instances.map((instance) => {
                const selected = instance.instanceId === selectedId;
                return (
                  <tr
                    key={instance.instanceId}
                    className={selected ? 'is-selected' : undefined}
                    tabIndex={0}
                    aria-selected={selected}
                    onClick={() => onSelect(instance.instanceId)}
                    onKeyDown={(event) => handleRowKeyDown(event, instance.instanceId)}
                  >
                    <td>
                      <span className="instance-name">{instance.name || '—'}</span>
                      <code className="instance-id">{instance.instanceId}</code>
                    </td>
                    <td>{instance.instanceType || '—'}</td>
                    <td>
                      <StatusBadge state={instance.state} />
                    </td>
                    <td>{instance.privateIp || '—'}</td>
                    <td>{instance.publicIp || '—'}</td>
                    <td title={formatDateTime(instance.launchTime)}>{formatAge(instance.launchTime)}</td>
                  </tr>
                );
              })}
              {!instances.length && !error && (
                <tr>
                  <td className="empty-row" colSpan={COLUMNS.length}>
                    No instances in this region.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
