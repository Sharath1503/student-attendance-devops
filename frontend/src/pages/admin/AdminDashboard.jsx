import { api } from '../../api.js';
import { formatPercent } from '../../utils.js';
import { Alert, DataTable, EmptyState, PageHeader, Spinner, StatCard, useFetch } from '../../components/ui.jsx';

export default function AdminDashboard() {
  const stats = useFetch(() => api.get('/admin/dashboard'));
  const audit = useFetch(() => api.get('/admin/audit?size=8'));

  return (
    <>
      <PageHeader title="Administrator dashboard" subtitle="Live figures from the database" />
      <Alert type="error">{stats.error}</Alert>
      {stats.loading ? (
        <Spinner />
      ) : (
        stats.data && (
          <div className="grid">
            <StatCard label="Students" value={stats.data.totalStudents} />
            <StatCard label="Faculty" value={stats.data.totalFaculty} />
            <StatCard label="Courses" value={stats.data.totalCourses} />
            <StatCard label="Classes held" value={stats.data.totalHeldSessions} hint={`${stats.data.sessionsLast7Days} in the last 7 days`} />
            <StatCard label="Overall attendance" value={formatPercent(stats.data.overallAttendancePercentage)} tone="ok" />
            <StatCard
              label="Students below threshold"
              value={stats.data.lowAttendanceStudents}
              tone={stats.data.lowAttendanceStudents > 0 ? 'danger' : 'ok'}
              hint={`Threshold ${stats.data.lowThresholdPercent}%`}
            />
          </div>
        )
      )}

      <div className="card">
        <h2>Recent activity</h2>
        <Alert type="error">{audit.error}</Alert>
        {audit.loading ? (
          <Spinner />
        ) : !audit.data || audit.data.content.length === 0 ? (
          <EmptyState title="No activity yet" />
        ) : (
          <DataTable caption="Recent audit records">
            <thead>
              <tr>
                <th>When</th>
                <th>User</th>
                <th>Action</th>
                <th>Details</th>
              </tr>
            </thead>
            <tbody>
              {audit.data.content.map((a) => (
                <tr key={a.id}>
                  <td>{new Date(a.createdAt).toLocaleString()}</td>
                  <td>{a.username || '—'}</td>
                  <td>{a.action}</td>
                  <td>{a.details}</td>
                </tr>
              ))}
            </tbody>
          </DataTable>
        )}
      </div>
    </>
  );
}
