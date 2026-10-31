import { useState } from 'react';
import { api } from '../api.js';
import { Alert, DataTable, EmptyState, Modal, PageHeader, PercentBadge, ProgressBar, Spinner, useFetch } from '../components/ui.jsx';

/** Attendance report for one course: per-student summary + list of class sessions. Used by admin and faculty. */
export default function CourseReport({ loadCourses }) {
  const courses = useFetch(loadCourses, []);
  const [courseId, setCourseId] = useState('');
  const [openSession, setOpenSession] = useState(null);

  return (
    <>
      <PageHeader title="Attendance reports" subtitle="Pick a course to see each student's attendance and every class held" />
      <Alert type="error">{courses.error}</Alert>
      {courses.loading ? (
        <Spinner />
      ) : (courses.data || []).length === 0 ? (
        <EmptyState title="No courses available">Courses appear here once they are created and assigned to you.</EmptyState>
      ) : (
        <div className="toolbar">
          <div className="field">
            <label htmlFor="course">Course</label>
            <select id="course" value={courseId} onChange={(e) => setCourseId(e.target.value)}>
              <option value="">Select a course…</option>
              {courses.data.map((c) => (
                <option key={c.id} value={c.id}>{c.code} — {c.name}</option>
              ))}
            </select>
          </div>
        </div>
      )}
      {courseId && <Report key={courseId} courseId={courseId} onOpen={setOpenSession} />}
      {openSession && <SessionModal courseId={courseId} sessionId={openSession} onClose={() => setOpenSession(null)} />}
    </>
  );
}

function Report({ courseId, onOpen }) {
  const summary = useFetch(() => api.get(`/attendance/courses/${courseId}/summary`), [courseId]);
  const sessions = useFetch(() => api.get(`/attendance/courses/${courseId}/sessions`), [courseId]);
  return (
    <>
      <div className="card">
        <h2>Student summary</h2>
        <Alert type="error">{summary.error}</Alert>
        {summary.loading ? (
          <Spinner />
        ) : summary.data.students.length === 0 ? (
          <EmptyState title="No students enrolled in this course" />
        ) : (
          <>
            <p className="muted">
              {summary.data.heldSessions} class(es) held · low-attendance threshold {summary.data.lowThresholdPercent}% · cancelled and holiday classes are not counted
            </p>
            <DataTable caption="Per-student attendance summary">
              <thead>
                <tr><th>ID</th><th>Name</th><th>Present</th><th>Absent</th><th>Total</th><th>Attendance</th><th></th></tr>
              </thead>
              <tbody>
                {summary.data.students.map((s) => (
                  <tr key={s.studentId}>
                    <td>{s.studentCode}</td><td>{s.studentName}</td><td>{s.present}</td><td>{s.absent}</td><td>{s.total}</td>
                    <td><PercentBadge value={s.percentage} threshold={summary.data.lowThresholdPercent} /></td>
                    <td><ProgressBar value={s.percentage} threshold={summary.data.lowThresholdPercent} /></td>
                  </tr>
                ))}
              </tbody>
            </DataTable>
          </>
        )}
      </div>
      <div className="card">
        <h2>Class history</h2>
        <Alert type="error">{sessions.error}</Alert>
        {sessions.loading ? (
          <Spinner />
        ) : sessions.data.length === 0 ? (
          <EmptyState title="No classes recorded yet" />
        ) : (
          <DataTable caption="Class sessions">
            <thead><tr><th>Date</th><th>Slot</th><th>Status</th><th>Present</th><th>Absent</th><th></th></tr></thead>
            <tbody>
              {sessions.data.map((s) => (
                <tr key={s.id}>
                  <td>{s.date}</td><td>{s.slot}</td>
                  <td><span className={`badge ${s.status === 'HELD' ? 'badge-ok' : 'badge-neutral'}`}>{s.status}</span></td>
                  <td>{s.presentCount}</td><td>{s.absentCount}</td>
                  <td><button type="button" className="btn btn-ghost btn-sm" onClick={() => onOpen(s.id)}>View</button></td>
                </tr>
              ))}
            </tbody>
          </DataTable>
        )}
      </div>
    </>
  );
}

function SessionModal({ courseId, sessionId, onClose }) {
  const detail = useFetch(() => api.get(`/attendance/courses/${courseId}/sessions/${sessionId}`), [courseId, sessionId]);
  return (
    <Modal title="Class attendance" onClose={onClose} footer={<button type="button" className="btn" onClick={onClose}>Close</button>}>
      <Alert type="error">{detail.error}</Alert>
      {detail.loading ? (
        <Spinner />
      ) : (
        <>
          <p><strong>{detail.data.session.date}</strong> · slot {detail.data.session.slot} · {detail.data.session.status}</p>
          {detail.data.records.length === 0 ? (
            <EmptyState title="No student records for this class" />
          ) : (
            <DataTable caption="Attendance for this class">
              <thead><tr><th>ID</th><th>Name</th><th>Status</th></tr></thead>
              <tbody>
                {detail.data.records.map((r) => (
                  <tr key={r.studentId}>
                    <td>{r.studentCode}</td><td>{r.studentName}</td>
                    <td><span className={`badge ${r.status === 'PRESENT' ? 'badge-present' : 'badge-absent'}`}>{r.status}</span></td>
                  </tr>
                ))}
              </tbody>
            </DataTable>
          )}
        </>
      )}
    </Modal>
  );
}
