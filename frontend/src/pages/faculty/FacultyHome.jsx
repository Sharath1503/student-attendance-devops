import { Link } from 'react-router-dom';
import { api } from '../../api.js';
import { Alert, EmptyState, PageHeader, Spinner, useFetch } from '../../components/ui.jsx';

export default function FacultyHome() {
  const courses = useFetch(() => api.get('/faculty/me/courses'), []);
  const rows = courses.data || [];
  return (
    <>
      <PageHeader title="My courses" subtitle="Courses you are authorised to manage" />
      <Alert type="error">{courses.error}</Alert>
      {courses.loading ? (
        <Spinner />
      ) : rows.length === 0 ? (
        <EmptyState title="No courses assigned yet">Ask the administrator to assign courses to your account.</EmptyState>
      ) : (
        <div className="course-cards">
          {rows.map((c) => (
            <div className="course-card" key={c.id}>
              <div>
                <strong>{c.code}</strong>
                <div>{c.name}</div>
                <div className="muted">{c.department}</div>
              </div>
              <span className={`badge ${c.active ? 'badge-ok' : 'badge-na'}`} style={{ alignSelf: 'flex-start' }}>
                {c.active ? 'Active' : 'Inactive'}
              </span>
              <div className="page-actions">
                <Link className="btn btn-sm" to={`/faculty/mark?course=${c.id}`}>Mark attendance</Link>
                <Link className="btn btn-ghost btn-sm" to="/faculty/reports">Reports</Link>
              </div>
            </div>
          ))}
        </div>
      )}
    </>
  );
}
