import { useCallback, useEffect, useRef, useState } from 'react';
import { formatPercent, percentTone } from '../utils.js';

/** Runs an async loader on mount / when deps change and tracks loading + error state. */
export function useFetch(loader, deps = []) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [tick, setTick] = useState(0);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError('');
    loader()
      .then((result) => {
        if (!cancelled) setData(result);
      })
      .catch((err) => {
        if (!cancelled) setError(err.message || 'Something went wrong');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, tick]);

  const reload = useCallback(() => setTick((t) => t + 1), []);
  return { data, loading, error, reload, setData };
}

export function Spinner({ label = 'Loading…' }) {
  return (
    <div className="spinner-wrap" role="status" aria-live="polite">
      <span className="spinner" aria-hidden="true" />
      <span>{label}</span>
    </div>
  );
}

export function Alert({ type = 'info', children, onClose }) {
  if (!children) return null;
  return (
    <div className={`alert alert-${type}`} role={type === 'error' ? 'alert' : 'status'}>
      <span>{children}</span>
      {onClose && (
        <button type="button" className="alert-close" onClick={onClose} aria-label="Dismiss message">
          ×
        </button>
      )}
    </div>
  );
}

export function EmptyState({ title, children }) {
  return (
    <div className="empty">
      <div className="empty-icon" aria-hidden="true">
        ∅
      </div>
      <strong>{title}</strong>
      {children && <p>{children}</p>}
    </div>
  );
}

export function PageHeader({ title, subtitle, actions }) {
  return (
    <div className="page-header">
      <div>
        <h1>{title}</h1>
        {subtitle && <p className="muted">{subtitle}</p>}
      </div>
      {actions && <div className="page-actions">{actions}</div>}
    </div>
  );
}

export function StatCard({ label, value, tone = 'neutral', hint }) {
  return (
    <div className={`stat stat-${tone}`}>
      <div className="stat-label">{label}</div>
      <div className="stat-value">{value}</div>
      {hint && <div className="stat-hint">{hint}</div>}
    </div>
  );
}

export function PercentBadge({ value, threshold }) {
  const tone = percentTone(value, threshold);
  return <span className={`badge badge-${tone}`}>{formatPercent(value)}</span>;
}

export function ProgressBar({ value, threshold }) {
  const tone = percentTone(value, threshold);
  const width = value === null || value === undefined ? 0 : Math.max(0, Math.min(100, value));
  return (
    <div
      className="progress"
      role="progressbar"
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={Math.round(width)}
      aria-label="Attendance"
    >
      <div className={`progress-bar progress-${tone}`} style={{ width: `${width}%` }} />
    </div>
  );
}

export function Modal({ title, onClose, children, footer }) {
  const ref = useRef(null);
  useEffect(() => {
    const previous = document.activeElement;
    const onKey = (e) => e.key === 'Escape' && onClose();
    document.addEventListener('keydown', onKey);
    ref.current?.focus();
    return () => {
      document.removeEventListener('keydown', onKey);
      previous?.focus?.();
    };
  }, [onClose]);

  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal" role="dialog" aria-modal="true" aria-label={title} tabIndex={-1} ref={ref}>
        <div className="modal-head">
          <h2>{title}</h2>
          <button type="button" className="icon-btn" onClick={onClose} aria-label="Close dialog">
            ×
          </button>
        </div>
        <div className="modal-body">{children}</div>
        {footer && <div className="modal-foot">{footer}</div>}
      </div>
    </div>
  );
}

export function Field({ label, name, error, hint, children }) {
  return (
    <div className={`field${error ? ' field-error' : ''}`}>
      <label htmlFor={name}>{label}</label>
      {children}
      {hint && !error && <small className="muted">{hint}</small>}
      {error && (
        <small className="error-text" id={`${name}-error`}>
          {error}
        </small>
      )}
    </div>
  );
}

export function Pagination({ page, totalPages, onChange }) {
  if (!totalPages || totalPages <= 1) return null;
  return (
    <nav className="pagination" aria-label="Pagination">
      <button type="button" className="btn btn-ghost" disabled={page <= 0} onClick={() => onChange(page - 1)}>
        ‹ Previous
      </button>
      <span>
        Page {page + 1} of {totalPages}
      </span>
      <button
        type="button"
        className="btn btn-ghost"
        disabled={page >= totalPages - 1}
        onClick={() => onChange(page + 1)}
      >
        Next ›
      </button>
    </nav>
  );
}

/** Table wrapper that scrolls horizontally on small screens. */
export function DataTable({ caption, children }) {
  return (
    <div className="table-wrap">
      <table>
        <caption className="sr-only">{caption}</caption>
        {children}
      </table>
    </div>
  );
}
