import { useCallback, useEffect, useRef, useState } from 'react';
import { api, describeError } from '../api/client';

export const AUTO_REFRESH_MS = 60000;

/**
 * Keeps a background refresh loop honest when responses arrive out of order: only the newest
 * request may write to state.
 */
function useSequencedRefresh() {
  const latest = useRef(0);
  const begin = useCallback(() => ++latest.current, []);
  const isCurrent = useCallback((id) => id === latest.current, []);
  return { begin, isCurrent };
}

/** EC2 inventory for the configured region. */
export function useInstances({ enabled = true, intervalMs = AUTO_REFRESH_MS } = {}) {
  const [instances, setInstances] = useState([]);
  const [status, setStatus] = useState('idle');
  const [error, setError] = useState('');
  const [updatedAt, setUpdatedAt] = useState(null);
  const { begin, isCurrent } = useSequencedRefresh();

  const refresh = useCallback(async () => {
    const requestId = begin();
    setStatus((current) => (current === 'ready' ? 'refreshing' : 'loading'));
    try {
      const { data } = await api.get('/monitoring/instances');
      if (!isCurrent(requestId)) return;
      setInstances(Array.isArray(data) ? data : []);
      setError('');
      setStatus('ready');
      setUpdatedAt(new Date());
    } catch (requestError) {
      if (!isCurrent(requestId)) return;
      setError(describeError(requestError, 'Could not load EC2 instances'));
      setStatus('error');
    }
  }, [begin, isCurrent]);

  useEffect(() => {
    if (!enabled) return undefined;
    refresh();
    if (!intervalMs) return undefined;
    const timer = window.setInterval(refresh, intervalMs);
    return () => window.clearInterval(timer);
  }, [enabled, intervalMs, refresh]);

  return {
    instances,
    error,
    updatedAt,
    loading: status === 'loading',
    refreshing: status === 'refreshing',
    refresh,
  };
}

/** CloudWatch CPU history for one instance over a look-back window in hours. */
export function useCpuSeries({ instanceId, hours, enabled = true, intervalMs = AUTO_REFRESH_MS }) {
  const [series, setSeries] = useState(null);
  const [status, setStatus] = useState('idle');
  const [error, setError] = useState('');
  const [updatedAt, setUpdatedAt] = useState(null);
  const { begin, isCurrent } = useSequencedRefresh();

  const refresh = useCallback(async () => {
    if (!instanceId) {
      setSeries(null);
      setStatus('idle');
      return;
    }
    const requestId = begin();
    setStatus((current) => (current === 'ready' ? 'refreshing' : 'loading'));
    try {
      const { data } = await api.get(`/monitoring/instances/${encodeURIComponent(instanceId)}/cpu`, {
        params: { hours },
      });
      if (!isCurrent(requestId)) return;
      setSeries(data);
      setError('');
      setStatus('ready');
      setUpdatedAt(new Date());
    } catch (requestError) {
      if (!isCurrent(requestId)) return;
      setSeries(null);
      setError(describeError(requestError, 'Could not load CPU history'));
      setStatus('error');
    }
  }, [begin, instanceId, hours, isCurrent]);

  useEffect(() => {
    if (!enabled) return undefined;
    refresh();
    if (!intervalMs) return undefined;
    const timer = window.setInterval(refresh, intervalMs);
    return () => window.clearInterval(timer);
  }, [enabled, intervalMs, refresh]);

  return {
    series,
    points: series?.points ?? [],
    error,
    updatedAt,
    loading: status === 'loading',
    refreshing: status === 'refreshing',
    refresh,
  };
}
