import { api } from '../../api.js';
import { formatPercent } from '../../utils.js';
import { Alert, DataTable, EmptyState, PageHeader, PercentBadge, ProgressBar, Spinner, StatCard, useFetch } from '../../components/ui.jsx';
import { percentTone } from '../../utils.js';

export default function StudentHome() {
  const summary = useFetch(() => api.get('/student/summary'), []);
  const d = summary.data;
  const tone = d ? { ok: 'ok', low: 'danger', na: 'neutral' }[percentTone(d.overallPercentage, d.lowThresholdPercent)] : 'neutral';

  return (
    <>
      <PageHeader title="My attendance" subtitle="Percentages count only classes that were held and recorded" />
      <Alert type="error">{summary.error}</Alert>
      {summary.loading ? (
        <Spinner />
      ) : (
        d && (
          <>
            {d.warnings.map((w) => (
              <Alert key={w} type="warn">{w}</Alert>
            ))}
            <div className="grid">
              <StatCard label="Overall attendance" value={formatPercent(d.overallPercentage)} tone={tone} hint={`${d.overallPresent} present of ${d.overallTotal} classes`} />
              <StatCard label="Courses" value={d.courses.length} />
              <StatCard label="Required minimum" value={`${d.lowThresholdPercent}%`} />
            </div>
            <div className="card">
              <h2>By course</h2>
              {d.courses.length === 0 ? (
                <EmptyState title="You are not enrolled in any course yet" />
              ) : (
                <DataTable caption="Attendance per course">
                  <thead><tr><th>Course</th><th>Present</th><th>Absent</th><th>Total</th><th>Attendance</th><th></th></tr></thead>
                  <tbody>
                    {d.courses.map((c) => (
                      <tr key={c.courseId}>
                        <td>{c.courseCode} — {c.courseName}</td>
                        <td>{c.present}</td><td>{c.absent}</td><td>{c.total}</td>
                        <td><PercentBadge value={c.percentage} threshold={d.lowThresholdPercent} /></td>
                        <td><ProgressBar value={c.percentage} threshold={d.lowThresholdPercent} /></td>
                      </tr>
                    ))}
                  </tbody>
                </DataTable>
              )}
            </div>
          </>
        )
      )}
    </>
  );
}
