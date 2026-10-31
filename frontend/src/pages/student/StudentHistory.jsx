import { useState } from 'react';
import { api } from '../../api.js';
import { toQuery } from '../../utils.js';
import { Alert, DataTable, EmptyState, PageHeader, Spinner, useFetch } from '../../components/ui.jsx';

export default function StudentHistory() {
  const courses = useFetch(() => api.get('/student/courses'), []);
  const [courseId, setCourseId] = useState('');
  const history = useFetch(() => api.get(`/student/attendance${toQuery({ courseId })}`), [courseId]);
  const rows = history.data || [];

  return (
    <>
      <PageHeader title="Attendance history" subtitle="Every class recorded for you, newest first" />
      <Alert type="error">{history.error || courses.error}</Alert>
      <div className="toolbar">
        <div className="field">
          <label htmlFor="course">Course</label>
          <select id="course" value={courseId} onChange={(e) => setCourseId(e.target.value)}>
            <option value="">All courses</option>
            {(courses.data || []).map((c) => (
              <option key={c.id} value={c.id}>{c.code} — {c.name}</option>
            ))}
          </select>
        </div>
      </div>
      {history.loading ? (
        <Spinner />
      ) : rows.length === 0 ? (
        <EmptyState title="No attendance recorded yet">Records appear here after your faculty marks a class.</EmptyState>
      ) : (
        <DataTable caption="Attendance history">
          <thead><tr><th>Date</th><th>Slot</th><th>Course</th><th>Status</th></tr></thead>
          <tbody>
            {rows.map((r, i) => (
              <tr key={`${r.courseId}-${r.date}-${r.slot}-${i}`}>
                <td>{r.date}</td><td>{r.slot}</td><td>{r.courseCode} — {r.courseName}</td>
                <td><span className={`badge ${r.status === 'PRESENT' ? 'badge-present' : 'badge-absent'}`}>{r.status}</span></td>
              </tr>
            ))}
          </tbody>
        </DataTable>
      )}
    </>
  );
}
