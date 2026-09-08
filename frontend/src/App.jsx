import { useEffect, useMemo, useState } from 'react';
import axios from 'axios';
import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts';

const API = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export default function App() {
  const [instances, setInstances] = useState([]);
  const [selected, setSelected] = useState('');
  const [metrics, setMetrics] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const loadInstances = async () => {
    try {
      setError('');
      const { data } = await axios.get(`${API}/monitoring/instances`);
      setInstances(data);
      if (!selected && data.length) setSelected(data[0].id);
    } catch (e) { setError('Unable to load AWS instances. Check the backend and AWS credentials.'); }
    finally { setLoading(false); }
  };

  const loadMetrics = async (id) => {
    if (!id) return;
    try {
      const { data } = await axios.get(`${API}/monitoring/instances/${id}/cpu?hours=3`);
      setMetrics(data);
    } catch { setMetrics(null); }
  };

  useEffect(() => { loadInstances(); }, []);
  useEffect(() => { loadMetrics(selected); }, [selected]);

  const chartData = useMemo(() => {
    if (!metrics) return [];
    return metrics.values.map((value, i) => ({ time: new Date(metrics.timestamps[i]).toLocaleTimeString([], {hour:'2-digit', minute:'2-digit'}), cpu: Number(value.toFixed(2)) }));
  }, [metrics]);

  const running = instances.filter(i => i.state === 'running').length;
  const stopped = instances.filter(i => i.state !== 'running').length;

  return <div className="app">
    <header><div><span className="eyebrow">AWS INFRASTRUCTURE</span><h1>Cloud Resource Monitor</h1><p>EC2 performance and CloudWatch telemetry</p></div><button onClick={loadInstances}>↻ Refresh</button></header>
    {error && <div className="error">{error}</div>}
    <section className="stats">
      <div><span>Instances</span><strong>{instances.length}</strong></div>
      <div><span>Running</span><strong>{running}</strong></div>
      <div><span>Stopped / other</span><strong>{stopped}</strong></div>
      <div><span>Region</span><strong>{import.meta.env.VITE_AWS_REGION || 'ap-south-1'}</strong></div>
    </section>
    <main>
      <section className="panel"><div className="panel-head"><div><h2>EC2 Instances</h2><p>{loading ? 'Loading resources…' : 'Select an instance to inspect its metrics.'}</p></div></div>
        <div className="table-wrap"><table><thead><tr><th>Instance</th><th>Type</th><th>Status</th><th>Private IP</th></tr></thead><tbody>
          {instances.map(i => <tr key={i.id} className={selected === i.id ? 'selected' : ''} onClick={() => setSelected(i.id)}><td><b>{i.id}</b></td><td>{i.type}</td><td><span className={`badge ${i.state}`}>{i.state}</span></td><td>{i.privateIp || '—'}</td></tr>)}
          {!instances.length && !loading && <tr><td colSpan="4" className="empty">No EC2 instances returned.</td></tr>}
        </tbody></table></div>
      </section>
      <section className="panel chart-panel"><div className="panel-head"><div><h2>CPU Utilization</h2><p>{selected || 'No instance selected'} · Last 3 hours</p></div></div>
        <div className="chart">{chartData.length ? <ResponsiveContainer width="100%" height="100%"><LineChart data={chartData}><CartesianGrid strokeDasharray="3 3"/><XAxis dataKey="time"/><YAxis unit="%"/><Tooltip/><Line type="monotone" dataKey="cpu" name="CPU" strokeWidth={2} dot={false}/></LineChart></ResponsiveContainer> : <div className="empty">Select an EC2 instance with CloudWatch data.</div>}</div>
      </section>
    </main>
  </div>;
}
