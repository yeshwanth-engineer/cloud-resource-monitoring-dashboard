import { formatDateTime } from '../lib/format';

export function DashboardHeader({ session, region, demoMode, updatedAt, refreshing, onRefresh, onLogout }) {
  return (
    <header className="app-header">
      <div>
        <p className="eyebrow">AWS infrastructure</p>
        <h1>Cloud Resource Monitor</h1>
        <p className="muted">
          EC2 inventory and CloudWatch CPU utilization
          {region ? ` · ${region}` : ''}
        </p>
      </div>

      <div className="header-side">
        <span className={`chip ${demoMode ? 'chip-warn' : 'chip-ok'}`}>
          {demoMode ? 'Demo data' : 'Live AWS'}
        </span>
        <p className="timestamp">
          {refreshing ? 'Refreshing…' : updatedAt ? `Updated ${formatDateTime(updatedAt)}` : 'Not loaded yet'}
        </p>
        <div className="header-actions">
          <button className="button" type="button" onClick={onRefresh} disabled={refreshing}>
            Refresh
          </button>
          <button className="button" type="button" onClick={onLogout}>
            Sign out{session?.username ? ` · ${session.username}` : ''}
          </button>
        </div>
      </div>
    </header>
  );
}
