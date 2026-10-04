import { Dashboard } from './pages/Dashboard';
import { LoginCard } from './components/LoginCard';
import { useAuth } from './hooks/useAuth';

/**
 * Top level switch between the sign-in card and the dashboard. The session lives in localStorage,
 * so a reload skips the login screen until the token expires or is revoked (401 → signed out).
 */
export default function App() {
  const { isAuthenticated, session, error, pending, login, logout } = useAuth();

  if (!isAuthenticated) {
    return (
      <LoginCard
        onLogin={login}
        pending={pending}
        error={error}
        hint="Demo build: use the account configured on the backend (DEMO_USERNAME / DEMO_PASSWORD, admin / admin123 by default)."
      />
    );
  }

  return <Dashboard session={session} onLogout={logout} />;
}
