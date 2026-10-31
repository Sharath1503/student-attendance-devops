import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../../api.js';
import { Alert, DataTable, EmptyState, PageHeader, Spinner, useFetch } from '../../components/ui.jsx';

export default function CourseDetail() {
  const { id } = useParams();
  const detail = useFetch(() => api.get(`/admin/courses/${id}`), [id]);
  const allFaculty = useFetch(() => api.get('/admin/faculty?size=100'), []);
  const allStudents = useFetch(() => api.get('/admin/students?size=100'), []);
  const [facultyId, setFacultyId] = useState('');
  const [studentId, setStudentId] = useState('');
  const [success, setSuccess] = useState('');
  const [failure, setFailure] = useState('');

  const run = async (action, okMessage) => {
    setSuccess('');
    setFailure('');
    try {
      await action();
      setSuccess(okMessage);
      detail.reload();
    } catch (err) {
      setFailure(err.message);
    }
  };

  if (detail.loading) return <Spinner />;
  if (detail.error) return <Alert type="error">{detail.error}</Alert>;
  const { course, faculty, students } = detail.data;

  const assignedFaculty = new Set(faculty.map((f) => f.id));
  const enrolled = new Set(students.map((s) => s.id));
  const facultyOptions = (allFaculty.data?.content || []).filter((f) => f.active && !assignedFaculty.has(f.id));
  const studentOptions = (allStudents.data?.content || []).filter((s) => s.active && !enrolled.has(s.id));

  return (
    <>
      <PageHeader
        title={`${course.code} — ${course.name}`}
        subtitle={`${course.department} · ${course.active ? 'Active' : 'Inactive'}`}
        actions={<Link className="btn btn-ghost" to="/admin/courses">← All courses</Link>}
      />
      <Alert type="success" onClose={() => setSuccess('')}>{success}</Alert>
      <Alert type="error" onClose={() => setFailure('')}>{failure}</Alert>

      <div className="card">
        <h2>Assigned faculty</h2>
        <div className="toolbar">
          <div className="field">
            <label htmlFor="faculty-select">Add faculty member</label>
            <select id="faculty-select" value={facultyId} onChange={(e) => setFacultyId(e.target.value)}>
              <option value="">Select…</option>
              {facultyOptions.map((f) => (
                <option key={f.id} value={f.id}>{f.fullName} ({f.facultyCode})</option>
              ))}
            </select>
          </div>
          <button
            type="button"
            className="btn"
            disabled={!facultyId}
            onClick={() => run(() => api.post(`/admin/courses/${id}/faculty/${facultyId}`), 'Faculty member assigned.').then(() => setFacultyId(''))}
          >
            Assign
          </button>
        </div>
        {faculty.length === 0 ? (
          <EmptyState title="No faculty assigned yet" />
        ) : (
          <DataTable caption="Assigned faculty">
            <thead><tr><th>ID</th><th>Name</th><th>Email</th><th>Actions</th></tr></thead>
            <tbody>
              {faculty.map((f) => (
                <tr key={f.id}>
                  <td>{f.facultyCode}</td><td>{f.fullName}</td><td>{f.email}</td>
                  <td><button type="button" className="btn btn-danger btn-sm" onClick={() => run(() => api.del(`/admin/courses/${id}/faculty/${f.id}`), 'Faculty member removed.')}>Remove</button></td>
                </tr>
              ))}
            </tbody>
          </DataTable>
        )}
      </div>

      <div className="card">
        <h2>Enrolled students ({students.length})</h2>
        <div className="toolbar">
          <div className="field">
            <label htmlFor="student-select">Enrol student</label>
            <select id="student-select" value={studentId} onChange={(e) => setStudentId(e.target.value)}>
              <option value="">Select…</option>
              {studentOptions.map((s) => (
                <option key={s.id} value={s.id}>{s.fullName} ({s.studentCode})</option>
              ))}
            </select>
          </div>
          <button
            type="button"
            className="btn"
            disabled={!studentId}
            onClick={() => run(() => api.post(`/admin/courses/${id}/students/${studentId}`), 'Student enrolled.').then(() => setStudentId(''))}
          >
            Enrol
          </button>
        </div>
        {students.length === 0 ? (
          <EmptyState title="No students enrolled yet" />
        ) : (
          <DataTable caption="Enrolled students">
            <thead><tr><th>ID</th><th>Name</th><th>Programme</th><th>Year</th><th>Actions</th></tr></thead>
            <tbody>
              {students.map((s) => (
                <tr key={s.id}>
                  <td>{s.studentCode}</td><td>{s.fullName}</td><td>{s.programme}</td><td>{s.academicYear}</td>
                  <td><button type="button" className="btn btn-danger btn-sm" onClick={() => run(() => api.del(`/admin/courses/${id}/students/${s.id}`), 'Student removed from the course.')}>Remove</button></td>
                </tr>
              ))}
            </tbody>
          </DataTable>
        )}
      </div>
    </>
  );
}
