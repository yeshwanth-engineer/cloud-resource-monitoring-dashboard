import { useCallback, useEffect, useState } from 'react';
import { api } from '../api/client';

/**
 * Public {@code /api/health} probe: region + demo mode, used for the header chips and to show a
 * clear banner when the API is unreachable.
 */
export function useHealth() {
  const [health, setHealth] = useState(null);
  const [reachable, setReachable] = useState(false);

  const check = useCallback(async () => {
    try {
      const { data } = await api.get('/health');
      setHealth(data);
      setReachable(true);
    } catch {
      setReachable(false);
    }
  }, []);

  useEffect(() => {
    check();
  }, [check]);

  return { health, reachable, region: health?.region, demoMode: Boolean(health?.demoMode) };
}
