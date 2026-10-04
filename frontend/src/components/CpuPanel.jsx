import { useMemo } from 'react';
import {
  CartesianGrid,
  Line,
  LineChart,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { formatClock, formatPercent, summarize } from '../lib/format';

export const HOUR_WINDOWS = [1, 3, 6, 12, 24];
const ALERT_THRESHOLD = 80;

export function CpuPanel({ instanceId, instanceName, hours, onHoursChange, points, source, loading, error, onRefresh }) {
  const chartData = useMemo(
    () =>
      points.map((point) => ({
        label: formatClock(point.timestamp),
        cpu: Number(point.value),
        timestamp: point.timestamp,
      })),
    [points],
  );

  const { average, latest, peak } = useMemo(() => summarize(points), [points]);
  const busy = loading && !chartData.length;

  return (
    <section className="card panel chart-panel" aria-label="CPU utilization">
      <div className="panel-head">
        <div>
          <h2>CPU utilization</h2>
          <p className="muted">
            {instanceName || 'No instance selected'}
            {instanceId ? ` · ${instanceId}` : ''}
            {source ? ` · ${source === 'AWS' ? 'CloudWatch average' : 'generated demo series'}` : ''}
          </p>
        </div>
        <div className="panel-tools">
          <div className="segmented" role="group" aria-label="Time window">
            {HOUR_WINDOWS.map((option) => (
              <button
                key={option}
                type="button"
                className={option === hours ? 'is-active' : undefined}
                aria-pressed={option === hours}
                onClick={() => onHoursChange(option)}
              >
                {option}h
              </button>
            ))}
          </div>
          <button className="button button-quiet" type="button" onClick={onRefresh}>
            Reload
          </button>
        </div>
      </div>

      <div className="chart-readouts">
        <Readout label="Latest" value={formatPercent(latest)} />
        <Readout label="Average" value={formatPercent(average)} />
        <Readout label="Peak" value={formatPercent(peak)} tone={peak >= ALERT_THRESHOLD ? 'warn' : undefined} />
      </div>

      {error ? (
        <p className="panel-note panel-note-error">{error}</p>
      ) : busy ? (
        <p className="panel-note">Loading metric data…</p>
      ) : chartData.length ? (
        <div className="chart">
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={chartData} margin={{ top: 10, right: 16, bottom: 4, left: -12 }}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="label" minTickGap={28} tickLine={false} axisLine={false} />
              <YAxis domain={[0, 100]} unit="%" width={54} tickLine={false} axisLine={false} />
              <Tooltip
                formatter={(value) => [`${value}%`, 'CPU']}
                labelFormatter={(label) => `Time ${label}`}
                contentStyle={{ borderRadius: 10, border: '1px solid #dbe1ea', fontSize: 13 }}
              />
              <ReferenceLine
                y={ALERT_THRESHOLD}
                stroke="#d1493f"
                strokeDasharray="4 4"
                label={{ value: `${ALERT_THRESHOLD}% alert`, position: 'insideTopRight', fontSize: 11, fill: '#d1493f' }}
              />
              <Line type="monotone" dataKey="cpu" name="CPU" stroke="#2563eb" strokeWidth={2} dot={false} activeDot={{ r: 4 }} isAnimationActive={false} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      ) : (
        <p className="panel-note">
          No datapoints for {instanceName || instanceId || 'this instance'} in the last {hours}h. Stopped
          instances report nothing, and a freshly launched one can take a few minutes to publish.
        </p>
      )}
    </section>
  );
}

function Readout({ label, value, tone }) {
  return (
    <div className="readout">
      <span>{label}</span>
      <strong className={tone ? `text-${tone}` : undefined}>{value}</strong>
    </div>
  );
}
