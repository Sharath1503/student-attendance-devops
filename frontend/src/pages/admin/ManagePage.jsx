import { useState } from 'react';
import { api } from '../../api.js';
import { isEmail, toQuery } from '../../utils.js';
import { Alert, DataTable, EmptyState, Field, Modal, PageHeader, Pagination, Spinner, useFetch } from '../../components/ui.jsx';

/**
 * Generic list + create + edit + deactivate screen used for both Students and Faculty.
 * `fields` describes the form, `columns` the table; everything is saved through the REST API.
 */
export default function ManagePage({ title, noun, endpoint, fields, columns }) {
  const [q, setQ] = useState('');
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const list = useFetch(() => api.get(`${endpoint}${toQuery({ q: query, page, size: 10 })}`), [endpoint, query, page]);
  const [editing, setEditing] = useState(null); // null | 'new' | row
  const [success, setSuccess] = useState('');
  const [failure, setFailure] = useState('');

  const rows = list.data?.content || [];

  const deactivate = async (row) => {
    if (!window.confirm(`Deactivate ${row.fullName}? They will no longer be able to sign in. Attendance history is kept.`)) return;
    setSuccess('');
    setFailure('');
    try {
      await api.del(`${endpoint}/${row.id}`);
      setSuccess(`${row.fullName} was deactivated.`);
      list.reload();
    } catch (err) {
      setFailure(err.message);
    }
  };

  return (
    <>
      <PageHeader
        title={title}
        subtitle={`Add, update and manage ${noun} records`}
        actions={
          <button type="button" className="btn" onClick={() => setEditing('new')}>
            + Add {noun}
          </button>
        }
      />
      <Alert type="success" onClose={() => setSuccess('')}>{success}</Alert>
      <Alert type="error" onClose={() => setFailure('')}>{failure || list.error}</Alert>

      <form
        className="toolbar"
        onSubmit={(e) => {
          e.preventDefault();
          setPage(0);
          setQuery(q.trim());
        }}
      >
        <div className="field">
          <label htmlFor="search">Search</label>
          <input id="search" value={q} onChange={(e) => setQ(e.target.value)} placeholder="Name, ID or department" />
        </div>
        <button type="submit" className="btn btn-ghost">Search</button>
      </form>

      {list.loading ? (
        <Spinner />
      ) : rows.length === 0 ? (
        <EmptyState title={`No ${noun} records found`}>{query ? 'Try a different search.' : `Use “Add ${noun}” to create the first one.`}</EmptyState>
      ) : (
        <DataTable caption={`${title} list`}>
          <thead>
            <tr>
              {columns.map(([, label]) => (
                <th key={label}>{label}</th>
              ))}
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.id}>
                {columns.map(([key]) => (
                  <td key={key}>{row[key]}</td>
                ))}
                <td>
                  <span className={`badge ${row.active ? 'badge-ok' : 'badge-na'}`}>{row.active ? 'Active' : 'Inactive'}</span>
                </td>
                <td className="actions">
                  <button type="button" className="btn btn-ghost btn-sm" onClick={() => setEditing(row)}>Edit</button>
                  {row.active && (
                    <button type="button" className="btn btn-danger btn-sm" onClick={() => deactivate(row)}>Deactivate</button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </DataTable>
      )}
      <Pagination page={page} totalPages={list.data?.totalPages} onChange={setPage} />

      {editing && (
        <EntityForm
          noun={noun}
          endpoint={endpoint}
          fields={fields}
          row={editing === 'new' ? null : editing}
          onClose={() => setEditing(null)}
          onSaved={(msg) => {
            setEditing(null);
            setSuccess(msg);
            list.reload();
          }}
        />
      )}
    </>
  );
}

function EntityForm({ noun, endpoint, fields, row, onClose, onSaved }) {
  const isNew = !row;
  const visible = fields.filter((f) => (isNew ? !f.editOnly : !f.createOnly));
  const [values, setValues] = useState(() => {
    const v = {};
    visible.forEach((f) => {
      v[f.name] = row ? row[f.name] ?? '' : '';
    });
    if (!isNew) v.active = row.active;
    return v;
  });
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);

  const validate = () => {
    const e = {};
    visible.forEach((f) => {
      const val = String(values[f.name] ?? '').trim();
      if (f.required && !val) e[f.name] = `${f.label} is required`;
      else if (val && f.type === 'email' && !isEmail(val)) e[f.name] = 'Enter a valid email address';
      else if (val && f.min && val.length < f.min) e[f.name] = `${f.label} must be at least ${f.min} characters`;
      else if (val && f.max && val.length > f.max) e[f.name] = `${f.label} must be at most ${f.max} characters`;
    });
    return e;
  };

  const submit = async (ev) => {
    ev.preventDefault();
    const e = validate();
    setErrors(e);
    setMessage('');
    if (Object.keys(e).length) return;
    setBusy(true);
    try {
      const body = {};
      visible.forEach((f) => {
        const val = values[f.name];
        body[f.name] = typeof val === 'string' ? val.trim() : val;
      });
      if (isNew) {
        await api.post(endpoint, body);
        onSaved(`The ${noun} was created.`);
      } else {
        body.active = values.active;
        if (!body.newPassword) delete body.newPassword;
        await api.put(`${endpoint}/${row.id}`, body);
        onSaved(`The ${noun} was updated.`);
      }
    } catch (err) {
      setErrors(err.fieldErrors || {});
      setMessage(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal
      title={isNew ? `Add ${noun}` : `Edit ${noun}`}
      onClose={onClose}
      footer={
        <>
          <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button type="submit" form="entity-form" className="btn" disabled={busy}>{busy ? 'Saving…' : 'Save'}</button>
        </>
      }
    >
      <Alert type="error">{message}</Alert>
      <form id="entity-form" onSubmit={submit} noValidate>
        <div className="form-grid">
          {visible.map((f) => (
            <Field key={f.name} label={f.label} name={f.name} error={errors[f.name]} hint={f.hint}>
              <input
                id={f.name}
                type={f.type || 'text'}
                value={values[f.name]}
                placeholder={f.placeholder}
                autoComplete={f.type === 'password' ? 'new-password' : 'off'}
                onChange={(e) => setValues({ ...values, [f.name]: e.target.value })}
                aria-invalid={!!errors[f.name]}
              />
            </Field>
          ))}
        </div>
        {!isNew && (
          <label className="inline-check">
            <input type="checkbox" checked={values.active} onChange={(e) => setValues({ ...values, active: e.target.checked })} />
            Account is active
          </label>
        )}
      </form>
    </Modal>
  );
}
