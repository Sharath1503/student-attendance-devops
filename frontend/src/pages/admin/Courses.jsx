import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../../api.js';
import { toQuery } from '../../utils.js';
import { Alert, DataTable, EmptyState, Field, Modal, PageHeader, Pagination, Spinner, useFetch } from '../../components/ui.jsx';

export default function Courses() {
  const [q, setQ] = useState('');
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const list = useFetch(() => api.get(`/admin/courses${toQuery({ q: query, page, size: 10 })}`), [query, page]);
  const [editing, setEditing] = useState(null);
  const [success, setSuccess] = useState('');
  const [failure, setFailure] = useState('');
  const rows = list.data?.content || [];

  const deactivate = async (c) => {
    if (!window.confirm(`Deactivate course ${c.code}? No new attendance can be recorded for it.`)) return;
    try {
      await api.del(`/admin/courses/${c.id}`);
      setSuccess(`${c.code} was deactivated.`);
      list.reload();
    } catch (err) {
      setFailure(err.message);
    }
  };

  return (
    <>
      <PageHeader
        title="Courses"
        subtitle="Create courses, then open one to assign faculty and enrol students"
        actions={<button type="button" className="btn" onClick={() => setEditing('new')}>+ Add course</button>}
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
          <input id="search" value={q} onChange={(e) => setQ(e.target.value)} placeholder="Code, name or department" />
        </div>
        <button type="submit" className="btn btn-ghost">Search</button>
      </form>
      {list.loading ? (
        <Spinner />
      ) : rows.length === 0 ? (
        <EmptyState title="No courses found">Use “Add course” to create the first one.</EmptyState>
      ) : (
        <DataTable caption="Course list">
          <thead>
            <tr><th>Code</th><th>Name</th><th>Department</th><th>Status</th><th>Actions</th></tr>
          </thead>
          <tbody>
            {rows.map((c) => (
              <tr key={c.id}>
                <td>{c.code}</td>
                <td>{c.name}</td>
                <td>{c.department}</td>
                <td><span className={`badge ${c.active ? 'badge-ok' : 'badge-na'}`}>{c.active ? 'Active' : 'Inactive'}</span></td>
                <td className="actions">
                  <Link className="btn btn-ghost btn-sm" to={`/admin/courses/${c.id}`}>Faculty &amp; students</Link>
                  <button type="button" className="btn btn-ghost btn-sm" onClick={() => setEditing(c)}>Edit</button>
                  {c.active && <button type="button" className="btn btn-danger btn-sm" onClick={() => deactivate(c)}>Deactivate</button>}
                </td>
              </tr>
            ))}
          </tbody>
        </DataTable>
      )}
      <Pagination page={page} totalPages={list.data?.totalPages} onChange={setPage} />
      {editing && (
        <CourseForm
          course={editing === 'new' ? null : editing}
          onClose={() => setEditing(null)}
          onSaved={(m) => {
            setEditing(null);
            setSuccess(m);
            list.reload();
          }}
        />
      )}
    </>
  );
}

function CourseForm({ course, onClose, onSaved }) {
  const [v, setV] = useState({
    code: course?.code || '',
    name: course?.name || '',
    department: course?.department || '',
    active: course ? course.active : true,
  });
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    const next = {};
    if (!v.code.trim()) next.code = 'Course code is required';
    if (!v.name.trim()) next.name = 'Course name is required';
    if (!v.department.trim()) next.department = 'Department is required';
    setErrors(next);
    setMessage('');
    if (Object.keys(next).length) return;
    setBusy(true);
    try {
      const body = { code: v.code.trim(), name: v.name.trim(), department: v.department.trim(), active: v.active };
      if (course) await api.put(`/admin/courses/${course.id}`, body);
      else await api.post('/admin/courses', body);
      onSaved(course ? 'The course was updated.' : 'The course was created.');
    } catch (err) {
      setErrors(err.fieldErrors || {});
      setMessage(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <Modal
      title={course ? 'Edit course' : 'Add course'}
      onClose={onClose}
      footer={
        <>
          <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button type="submit" form="course-form" className="btn" disabled={busy}>{busy ? 'Saving…' : 'Save'}</button>
        </>
      }
    >
      <Alert type="error">{message}</Alert>
      <form id="course-form" onSubmit={submit} noValidate>
        <Field label="Course code" name="code" error={errors.code}>
          <input id="code" value={v.code} onChange={(e) => setV({ ...v, code: e.target.value })} />
        </Field>
        <Field label="Course name" name="name" error={errors.name}>
          <input id="name" value={v.name} onChange={(e) => setV({ ...v, name: e.target.value })} />
        </Field>
        <Field label="Department" name="department" error={errors.department}>
          <input id="department" value={v.department} onChange={(e) => setV({ ...v, department: e.target.value })} />
        </Field>
        <label className="inline-check">
          <input type="checkbox" checked={v.active} onChange={(e) => setV({ ...v, active: e.target.checked })} />
          Course is active
        </label>
      </form>
    </Modal>
  );
}
