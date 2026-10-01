import { BrowserRouter, Link, Navigate, NavLink, Route, Routes } from 'react-router-dom';
import { LogOut, Shield } from 'lucide-react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { LandingPage } from './pages/LandingPage';
import { DashboardPage } from './pages/DashboardPage';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { ArchitecturePage } from './pages/ArchitecturePage';
import { ShelterFormPage } from './pages/ShelterFormPage';

function ProtectedRoute({ children }) {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? children : <Navigate to="/login" replace />;
}

function AppShell() {
  const { isAuthenticated, user, logout } = useAuth();

  const navLinkClass = ({ isActive }) =>
    `inline-flex items-center gap-2 rounded-full px-3 py-2 text-sm font-medium transition ${
      isActive ? 'bg-primary/10 text-primary' : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
    }`;

  return (
    <div className="min-h-screen bg-background text-slate-800">
      <header className="sticky top-0 z-30 border-b border-slate-200 bg-white/80 backdrop-blur-xl">
        <div className="mx-auto flex max-w-7xl items-center justify-between gap-4 px-4 py-4 md:px-8">
          <Link to="/" className="flex items-center gap-3 font-heading text-xl tracking-tight text-slate-900">
            <span className="inline-flex h-9 w-9 items-center justify-center rounded-xl bg-primary/10 text-primary">
              <Shield className="h-5 w-5" />
            </span>
            SAHAYAK
          </Link>

          <nav className="hidden items-center gap-2 md:flex">
            <NavLink to="/" className={navLinkClass}>Home</NavLink>
            <NavLink to="/dashboard" className={navLinkClass}>Dashboard</NavLink>
            <NavLink to="/architecture" className={navLinkClass}>Architecture</NavLink>
            {isAuthenticated && <NavLink to="/shelters/new" className={navLinkClass}>Add Shelter</NavLink>}
          </nav>

          <div className="flex items-center gap-3">
            {isAuthenticated ? (
              <>
                <span className="hidden rounded-full border border-slate-200 bg-slate-50 px-3 py-1.5 text-sm font-medium text-slate-700 md:inline-flex">
                  {user?.username}
                </span>
                <button onClick={logout} className="inline-flex items-center gap-2 rounded-full border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50">
                  <LogOut className="h-4 w-4" /> Logout
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className="rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50">
                  Login
                </Link>
                <Link to="/register" className="rounded-full bg-primary px-4 py-2 text-sm font-semibold text-white hover:bg-blue-600">
                  Register
                </Link>
              </>
            )}
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-7xl px-4 py-8 md:px-8">
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/architecture" element={<ArchitecturePage />} />
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <DashboardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/shelters/new"
            element={
              <ProtectedRoute>
                <ShelterFormPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/shelters/:id/edit"
            element={
              <ProtectedRoute>
                <ShelterFormPage />
              </ProtectedRoute>
            }
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <AppShell />
      </BrowserRouter>
    </AuthProvider>
  );
}
