import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export function RegisterPage() {
  const navigate = useNavigate();
  const { register, isAuthenticated, loading } = useAuth();
  const [form, setForm] = useState({ username: '', password: '' });
  const [error, setError] = useState('');

  if (isAuthenticated) {
    navigate('/dashboard');
    return null;
  }

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');

    try {
      await register(form.username, form.password);
      navigate('/dashboard');
    } catch (err) {
      setError(err.message || 'Registration failed.');
    }
  };

  return (
    <div className="mx-auto max-w-md rounded-[30px] border border-slate-200 bg-white p-8 shadow-sm">
      <div className="mb-6">
        <p className="text-sm font-semibold uppercase tracking-[0.18em] text-slate-500">Create account</p>
        <h1 className="mt-2 font-heading text-4xl text-slate-900">Join SAHAYAK</h1>
      </div>

      <form onSubmit={handleSubmit} className="space-y-5">
        <div>
          <label className="mb-1 block text-sm font-medium text-slate-700">Username</label>
          <input name="username" value={form.username} onChange={handleChange} className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 outline-none ring-0 transition focus:border-primary focus:bg-white" placeholder="reliefworker" required />
        </div>
        <div>
          <label className="mb-1 block text-sm font-medium text-slate-700">Password</label>
          <input type="password" name="password" value={form.password} onChange={handleChange} className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 outline-none ring-0 transition focus:border-primary focus:bg-white" placeholder="Minimum 8 characters" required />
        </div>

        {error && <div className="rounded-2xl border border-red-200 bg-red-50 p-3 text-sm text-red-700">{error}</div>}

        <button type="submit" disabled={loading} className="w-full rounded-full bg-primary px-4 py-3 text-sm font-semibold text-white hover:bg-blue-600 disabled:opacity-60">
          {loading ? 'Creating account...' : 'Create account'}
        </button>
      </form>

      <p className="mt-5 text-sm text-slate-600">
        Already signed in? <Link to="/login" className="font-semibold text-primary">Log in</Link>
      </p>
    </div>
  );
}
