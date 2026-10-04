const timeFormatter = new Intl.DateTimeFormat(undefined, { hour: '2-digit', minute: '2-digit' });
const dateTimeFormatter = new Intl.DateTimeFormat(undefined, {
  dateStyle: 'medium',
  timeStyle: 'short',
});

export function formatClock(value) {
  const date = toDate(value);
  return date ? timeFormatter.format(date) : '—';
}

export function formatDateTime(value) {
  const date = toDate(value);
  return date ? dateTimeFormatter.format(date) : '—';
}

/** "3 days ago" / "5 hours ago" style label for the launch time column. */
export function formatAge(value) {
  const date = toDate(value);
  if (!date) return '—';
  const seconds = Math.round((Date.now() - date.getTime()) / 1000);
  if (seconds < 60) return 'just now';

  const units = [
    ['year', 31536000],
    ['month', 2592000],
    ['day', 86400],
    ['hour', 3600],
    ['minute', 60],
  ];
  for (const [unit, unitSeconds] of units) {
    const amount = Math.floor(seconds / unitSeconds);
    if (amount >= 1) {
      return new Intl.RelativeTimeFormat(undefined, { numeric: 'always' }).format(-amount, unit);
    }
  }
  return 'just now';
}

export function formatPercent(value, digits = 1) {
  return typeof value === 'number' && Number.isFinite(value) ? `${value.toFixed(digits)}%` : '—';
}

/** Average, newest and highest value of a series, used for the chart headline. */
export function summarize(points) {
  if (!points?.length) {
    return { average: null, latest: null, peak: null };
  }
  const values = points.map((point) => point.value);
  const sum = values.reduce((total, value) => total + value, 0);
  return {
    average: sum / values.length,
    latest: values[values.length - 1],
    peak: Math.max(...values),
  };
}

function toDate(value) {
  if (!value) return null;
  const date = value instanceof Date ? value : new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}
