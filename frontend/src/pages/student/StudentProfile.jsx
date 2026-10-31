import { api } from '../../api.js';
import { Alert, EmptyState, PageHeader, Spinner, useFetch } from '../../components/ui.jsx';

export default function StudentProfile() {
  const profile = useFetch(() => api.get('/student/profile'), []);
  const courses = useFetch(() => api.get('/student/courses'), []);
  const p = profile.data;
  const rows = [
    ['Student ID', p?.studentCode],
    ['Name', p?.fullName],
    ['Email', p?.email],
    ['Department', p?.department],
    ['Course / programme', p?.programme],
    ['Academic year', p?.academicYear],
    ['Username', p?.username],
  ];
  return (
    <>
      <PageHeader title="My profile" />
      <Alert type="error">{profile.error || courses.error}</Alert>
      {profile.loading ? (
        <Spinner />
      ) : (
        <div className="card">
          <dl style={{ display: 'grid', gridTemplateColumns: 'max-content 1fr', gap: '0.5rem 1.5rem', margin: 0 }}>
            {rows.map(([k, v]) => (
              <div key={k} style={{ display: 'contents' }}>
                <dt className="muted">{k}</dt>
                <dd style={{ margin: 0 }}>{v}</dd>
              </div>
            ))}
          </dl>
        </div>
      )}
      <div className="card">
        <h2>Enrolled courses</h2>
        {courses.loading ? (
          <Spinner />
        ) : (courses.data || []).length === 0 ? (
          <EmptyState title="Not enrolled in any course yet" />
        ) : (
          <ul>
            {courses.data.map((c) => (
              <li key={c.id}>{c.code} — {c.name}</li>
            ))}
          </ul>
        )}
      </div>
    </>
  );
}
