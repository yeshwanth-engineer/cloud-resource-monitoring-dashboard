import { useState } from 'react';
import { Banner } from './Banner';

/**
 * Sign-in form for the demo account. Credentials come from the backend's `app.demo-auth.*`
 * settings, so there is nothing secret pre-filled here beyond the documented demo default.
 */
export function LoginCard({ onLogin, pending, error, hint }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');

  async function handleSubmit(event) {
    event.preventDefault();
    if (pending) return;
    await onLogin(username.trim(), password);
  }

  return (
    <main className="login-shell">
      <form className="card login-card" onSubmit={handleSubmit}>
        <p className="eyebrow">AWS infrastructure</p>
        <h1>Cloud Resource Monitor</h1>
        <p className="muted">Sign in to inspect EC2 instances and CloudWatch CPU history.</p>

        <label htmlFor="username">Username</label>
        <input
          id="username"
          autoComplete="username"
          value={username}
          onChange={(event) => setUsername(event.target.value)}
          placeholder="admin"
          required
        />

        <label htmlFor="password">Password</label>
        <input
          id="password"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          placeholder="••••••••"
          required
        />

        <button className="button button-primary" type="submit" disabled={pending}>
          {pending ? 'Signing in…' : 'Sign in'}
        </button>

        <Banner>{error}</Banner>
        {hint && <p className="hint">{hint}</p>}
      </form>
    </main>
  );
}
