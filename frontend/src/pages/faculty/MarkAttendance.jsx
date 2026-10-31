import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api } from '../../api.js';
import { todayISO } from '../../utils.js';
import { Alert, DataTable, EmptyState, Field, PageHeader, Spinner, useFetch } from '../../components/ui.jsx';

const WINDOW_DAYS = 7; // keep in sync with ATTENDANCE_EDIT_WINDOW_DAYS (the server enforces the real rule)

export default function MarkAttendance() {
  const [params] = useSearchParams();
  const courses = useFetch(() => api.get('/faculty/me/courses'), []);
  const [courseId, setCourseId] = useState(params.get('course') || '');
  const [date, setDate] = useState(todayISO());
  const [slot, setSlot] = useState(1);
  const [status, setStatus] = useState('HELD');
  const [marks, setMarks] = useState({}); // studentId -> PRESENT | ABSENT
  const [editingExisting, setEditingExisting] = useState(false);
  const [busy, setBusy] = useState(false);
  const [success, setSuccess] = useState('');
  const [failure, setFailure] = useState('');
  const [loadingExisting, setLoadingExisting] = useState(false);

  const roster = useFetch(
    () => (courseId ? api.get(`/attendance/courses/${courseId}/students`) : Promise.resolve([])),
    [courseId],
  );
  const students = (roster.data || []).filter((s) => s.active);

  // When course/date/slot changes, load an existing session (if any) so it can be edited instead of re-created.
  useEffect(() => {
    if (!courseId) return undefined;
    let cancelled = false;
    setLoadingExisting(true);
    setEditingExisting(false);
    api
      .get(`/attendance/courses/${courseId}/sessions`)
      .then(async (list) => {
        const found = list.find((s) => s.date === date && s.slot === Number(slot));
        if (!found) {
          if (!cancelled) setMarks({});
          return;
        }
        const detail = await api.get(`/attendance/courses/${courseId}/sessions/${found.id}`);
        if (cancelled) return;
        const m = {};
        detail.records.forEach((r) => {
          m[r.studentId] = r.status;
        });
        setMarks(m);
        setStatus(detail.session.status);
        setEditingExisting(true);
      })
      .catch((err) => !cancelled && setFailure(err.message))
      .finally(() => !cancelled && setLoadingExisting(false));
    return () => {
      cancelled = true;
    };
  }, [courseId, date, slot]);

  const setAll = (value) => {
    const m = {};
    students.forEach((s) => {
      m[s.id] = value;
    });
    setMarks(m);
  };

  const save = async () => {
    setSuccess('');
    setFailure('');
    let records = [];
    if (status === 'HELD') {
      const missing = students.filter((s) => !marks[s.id]);
      if (missing.length) {
        setFailure(`Please mark every student (${missing.length} not marked yet), or use “Mark all present”.`);
        return;
      }
      records = students.map((s) => ({ studentId: s.id, status: marks[s.id] }));
    }
    setBusy(true);
    try {
      const saved = await api.post(`/attendance/courses/${courseId}/sessions`, { date, slot: Number(slot), status, records });
      setEditingExisting(true);
      setSuccess(
        status === 'HELD'
          ? `Attendance saved: ${saved.session.presentCount} present, ${saved.session.absentCount} absent.`
          : `The class was recorded as ${status.toLowerCase()}; it is not counted in attendance percentages.`,
      );
    } catch (err) {
      setFailure(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <PageHeader title="Mark attendance" subtitle={`Record a class, or correct one from the last ${WINDOW_DAYS} days`} />
      <Alert type="success" onClose={() => setSuccess('')}>{success}</Alert>
      <Alert type="error" onClose={() => setFailure('')}>{failure || courses.error || roster.error}</Alert>

      {courses.loading ? (
        <Spinner />
      ) : (courses.data || []).length === 0 ? (
        <EmptyState title="No courses assigned yet">Ask the administrator to assign courses to your account.</EmptyState>
      ) : (
        <div className="card">
          <div className="form-grid">
            <Field label="Course" name="course">
              <select id="course" value={courseId} onChange={(e) => { setCourseId(e.target.value); setMarks({}); }}>
                <option value="">Select a course…</option>
                {courses.data.filter((c) => c.active).map((c) => (
                  <option key={c.id} value={c.id}>{c.code} — {c.name}</option>
                ))}
              </select>
            </Field>
            <Field label="Date" name="date">
              <input id="date" type="date" value={date} min={todayISO(-WINDOW_DAYS)} max={todayISO()} onChange={(e) => setDate(e.target.value)} />
            </Field>
            <Field label="Slot / period" name="slot" hint="Use different slots for several classes on one day">
              <select id="slot" value={slot} onChange={(e) => setSlot(e.target.value)}>
                {[1, 2, 3, 4, 5, 6].map((n) => <option key={n} value={n}>{n}</option>)}
              </select>
            </Field>
            <Field label="Class status" name="status">
              <select id="status" value={status} onChange={(e) => setStatus(e.target.value)}>
                <option value="HELD">Held</option>
                <option value="CANCELLED">Cancelled</option>
                <option value="HOLIDAY">Holiday</option>
              </select>
            </Field>
          </div>
          {editingExisting && <Alert type="info">A record already exists for this date and slot. Saving will update it.</Alert>}
        </div>
      )}

      {courseId && (roster.loading || loadingExisting) && <Spinner />}

      {courseId && !roster.loading && !loadingExisting && status === 'HELD' && (
        students.length === 0 ? (
          <EmptyState title="No students enrolled in this course">The administrator can enrol students from the Courses page.</EmptyState>
        ) : (
          <>
            <div className="page-actions" style={{ marginBottom: 12 }}>
              <button type="button" className="btn btn-ghost btn-sm" onClick={() => setAll('PRESENT')}>Mark all present</button>
              <button type="button" className="btn btn-ghost btn-sm" onClick={() => setAll('ABSENT')}>Mark all absent</button>
            </div>
            <DataTable caption="Students to mark">
              <thead><tr><th>ID</th><th>Name</th><th>Attendance</th></tr></thead>
              <tbody>
                {students.map((s) => (
                  <tr key={s.id}>
                    <td>{s.studentCode}</td>
                    <td>{s.fullName}</td>
                    <td>
                      <div className="seg" role="group" aria-label={`Attendance for ${s.fullName}`}>
                        <button type="button" className="p" aria-pressed={marks[s.id] === 'PRESENT'} onClick={() => setMarks({ ...marks, [s.id]: 'PRESENT' })}>Present</button>
                        <button type="button" className="a" aria-pressed={marks[s.id] === 'ABSENT'} onClick={() => setMarks({ ...marks, [s.id]: 'ABSENT' })}>Absent</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </DataTable>
          </>
        )
      )}

      {courseId && !roster.loading && !loadingExisting && (
        <div style={{ marginTop: 16 }}>
          <button type="button" className="btn" disabled={busy || (status === 'HELD' && students.length === 0)} onClick={save}>
            {busy ? 'Saving…' : editingExisting ? 'Update attendance' : 'Save attendance'}
          </button>
        </div>
      )}
    </>
  );
}
