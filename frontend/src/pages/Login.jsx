import { useState } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth.jsx';
import { homePathFor } from '../utils.js';
import { Alert, Field } from '../components/ui.jsx';

export default function Login() {
  const { user, login, notice } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ username: '', password: '' });
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to={homePathFor(user.role)} replace />;

  const submit = async (e) => {
    e.preventDefault();
    const next = {};
    if (!form.username.trim()) next.username = 'Username is required';
    if (!form.password) next.password = 'Password is required';
    setErrors(next);
    setMessage('');
    if (Object.keys(next).length) return;
    setBusy(true);
    try {
      const u = await login(form.username.trim(), form.password);
      navigate(homePathFor(u.role), { replace: true });
    } catch (err) {
      setMessage(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="login-page">
      <form className="login-card" onSubmit={submit} noValidate>
        <h1>Student Attendance</h1>
        <p className="muted" style={{ marginBottom: 18 }}>
          Sign in with the account given to you by the administrator.
        </p>
        {notice && <Alert type="info">{notice}</Alert>}
        <Alert type="error">{message}</Alert>
        <Field label="Username" name="username" error={errors.username}>
          <input
            id="username"
            autoComplete="username"
            autoFocus
            value={form.username}
            onChange={(e) => setForm({ ...form, username: e.target.value })}
            aria-invalid={!!errors.username}
          />
        </Field>
        <Field label="Password" name="password" error={errors.password}>
          <input
            id="password"
            type="password"
            autoComplete="current-password"
            value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
            aria-invalid={!!errors.password}
          />
        </Field>
        <button type="submit" className="btn" disabled={busy}>
          {busy ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
    </div>
  );
}
