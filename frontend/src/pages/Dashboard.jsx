import { lazy, Suspense, useEffect, useMemo, useState } from 'react';
import { api } from '../api/client';
import { Banner } from '../components/Banner';
import { DashboardHeader } from '../components/DashboardHeader';
import { InstanceTable } from '../components/InstanceTable';
import { SummaryStats } from '../components/SummaryStats';
import { useHealth } from '../hooks/useHealth';
import { useCpuSeries, useInstances } from '../hooks/useMonitoring';

// recharts is by far the heaviest dependency; loading the chart on demand keeps the initial
// bundle to the table and stat cards.
const CpuPanel = lazy(() =>
  import('../components/CpuPanel').then((module) => ({ default: module.CpuPanel })),
);

export function Dashboard({ session, onLogout }) {
  const [selectedId, setSelectedId] = useState('');
  const [hours, setHours] = useState(3);

  const { region, demoMode, reachable } = useHealth();
  const { instances, loading, refreshing, error, updatedAt, refresh } = useInstances();

  const selected = useMemo(
    () => instances.find((instance) => instance.instanceId === selectedId) ?? null,
    [instances, selectedId],
  );

  // Keep a row selected: prefer a running instance so the chart has datapoints, and drop the
  // selection if the instance disappeared from the inventory after a refresh.
  useEffect(() => {
    if (!instances.length) return;
    if (instances.some((instance) => instance.instanceId === selectedId)) return;
    const fallback = instances.find((instance) => instance.state === 'running') ?? instances[0];
    setSelectedId(fallback.instanceId);
  }, [instances, selectedId]);

  const { series, points, error: metricsError, loading: metricsLoading, refresh: refreshMetrics } = useCpuSeries({
    instanceId: selected?.instanceId ?? '',
    hours,
  });

  async function refreshAll() {
    await Promise.all([refresh(), refreshMetrics()]);
  }

  return (
    <div className="app-shell">
      <DashboardHeader
        session={session}
        region={region}
        demoMode={demoMode}
        updatedAt={updatedAt}
        refreshing={refreshing}
        onRefresh={refreshAll}
        onLogout={onLogout}
      />

      {!reachable && (
        <Banner tone="error">
          The API at <code>{api.defaults.baseURL}</code> is not reachable. Start it with{' '}
          <code>make backend-run</code>, or run the stub with <code>npm run mock:api</code> in{' '}
          <code>frontend/</code>.
        </Banner>
      )}
      {reachable && demoMode && (
        <Banner tone="info">
          The backend is serving generated instances because <code>DEMO_MODE=true</code>. Export AWS
          credentials and set <code>DEMO_MODE=false</code> to read your real account.
        </Banner>
      )}
      {error && <Banner tone="error">{error}</Banner>}

      <SummaryStats
        instances={instances}
        points={points}
        source={series?.source}
        selectedName={selected?.name || selected?.instanceId}
      />

      <main className="grid">
        <InstanceTable
          instances={instances}
          selectedId={selectedId}
          loading={loading}
          error={error}
          onSelect={setSelectedId}
        />
        <Suspense fallback={<section className="card panel chart-panel"><p className="panel-note">Loading chart…</p></section>}>
          <CpuPanel
            instanceId={selected?.instanceId}
            instanceName={selected?.name}
            hours={hours}
            onHoursChange={setHours}
            points={points}
            source={series?.source}
            loading={metricsLoading}
            error={metricsError}
            onRefresh={refreshMetrics}
          />
        </Suspense>
      </main>

      <footer className="app-footer">
        <span>
          Spring Boot <code>/api/monitoring</code> · React · {region ? `region ${region}` : 'region unknown'}
        </span>
        <span>{demoMode ? 'demo mode' : 'live AWS data'}</span>
      </footer>
    </div>
  );
}
