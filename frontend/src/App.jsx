import { useState } from 'react';
import { HashRouter, Navigate, NavLink, Outlet, Route, Routes } from 'react-router-dom';
import { AuthProvider, useAuth } from './auth.jsx';
import { homePathFor } from './utils.js';
import Login from './pages/Login.jsx';
import AdminDashboard from './pages/admin/AdminDashboard.jsx';
import ManagePage from './pages/admin/ManagePage.jsx';
import Courses from './pages/admin/Courses.jsx';
import CourseDetail from './pages/admin/CourseDetail.jsx';
import CourseReport from './pages/CourseReport.jsx';
import FacultyHome from './pages/faculty/FacultyHome.jsx';
import MarkAttendance from './pages/faculty/MarkAttendance.jsx';
import StudentHome from './pages/student/StudentHome.jsx';
import StudentHistory from './pages/student/StudentHistory.jsx';
import StudentProfile from './pages/student/StudentProfile.jsx';
import { api } from './api.js';

const NAV = {
  ADMIN: [
    ['/admin', 'Dashboard', true],
    ['/admin/students', 'Students'],
    ['/admin/faculty', 'Faculty'],
    ['/admin/courses', 'Courses'],
    ['/admin/reports', 'Attendance reports'],
  ],
  FACULTY: [
    ['/faculty', 'My courses', true],
    ['/faculty/mark', 'Mark attendance'],
    ['/faculty/reports', 'Attendance reports'],
  ],
  STUDENT: [
    ['/student', 'My attendance', true],
    ['/student/history', 'History'],
    ['/student/profile', 'Profile'],
  ],
};

function Layout() {
  const { user, logout } = useAuth();
  const [open, setOpen] = useState(false);
  return (
    <div className="shell">
      <div className="topbar">
        <strong>Attendance System</strong>
        <button type="button" className="btn btn-ghost" onClick={() => setOpen((o) => !o)} aria-expanded={open}>
          Menu
        </button>
      </div>
      <aside className={`sidebar${open ? ' open' : ''}`}>
        <div className="brand">
          Attendance System
          <small>{user.role.toLowerCase()} portal</small>
        </div>
        <nav className="nav" aria-label="Main" onClick={() => setOpen(false)}>
          {NAV[user.role].map(([to, label, end]) => (
            <NavLink key={to} to={to} end={end}>
              {label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-foot">
          <div>{user.displayName}</div>
          <div className="muted">@{user.username}</div>
          <button type="button" className="btn btn-ghost btn-sm" style={{ marginTop: 8, color: '#fff' }} onClick={logout}>
            Sign out
          </button>
        </div>
      </aside>
      <main className="content" id="main">
        <Outlet />
      </main>
    </div>
  );
}

function Protected() {
  const { user } = useAuth();
  return user ? <Layout /> : <Navigate to="/login" replace />;
}

function RoleGate({ role }) {
  const { user } = useAuth();
  return user.role === role ? <Outlet /> : <Navigate to={homePathFor(user.role)} replace />;
}

function HomeRedirect() {
  const { user } = useAuth();
  return <Navigate to={homePathFor(user.role)} replace />;
}

const STUDENT_FIELDS = [
  { name: 'studentCode', label: 'Student ID', required: true, createOnly: true, max: 30 },
  { name: 'fullName', label: 'Full name', required: true, max: 100 },
  { name: 'email', label: 'Email', type: 'email', required: true },
  { name: 'department', label: 'Department', required: true },
  { name: 'programme', label: 'Course / programme', required: true, placeholder: 'e.g. MCA' },
  { name: 'academicYear', label: 'Academic year', required: true, placeholder: 'e.g. 2025-26' },
  { name: 'username', label: 'Login username', required: true, createOnly: true, min: 3 },
  { name: 'password', label: 'Initial password', type: 'password', required: true, createOnly: true, min: 8 },
  { name: 'newPassword', label: 'New password (optional)', type: 'password', editOnly: true, min: 8, hint: 'Leave blank to keep the current password' },
];
const STUDENT_COLUMNS = [
  ['studentCode', 'Student ID'],
  ['fullName', 'Name'],
  ['department', 'Department'],
  ['programme', 'Course'],
  ['academicYear', 'Year'],
];

const FACULTY_FIELDS = [
  { name: 'facultyCode', label: 'Faculty ID', required: true, createOnly: true, max: 30 },
  { name: 'fullName', label: 'Full name', required: true, max: 100 },
  { name: 'email', label: 'Email', type: 'email', required: true },
  { name: 'department', label: 'Department', required: true },
  { name: 'username', label: 'Login username', required: true, createOnly: true, min: 3 },
  { name: 'password', label: 'Initial password', type: 'password', required: true, createOnly: true, min: 8 },
  { name: 'newPassword', label: 'New password (optional)', type: 'password', editOnly: true, min: 8, hint: 'Leave blank to keep the current password' },
];
const FACULTY_COLUMNS = [
  ['facultyCode', 'Faculty ID'],
  ['fullName', 'Name'],
  ['email', 'Email'],
  ['department', 'Department'],
];

export default function App() {
  return (
    <HashRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route element={<Protected />}>
            <Route index element={<HomeRedirect />} />
            <Route path="admin" element={<RoleGate role="ADMIN" />}>
              <Route index element={<AdminDashboard />} />
              <Route
                path="students"
                element={<ManagePage title="Students" noun="student" endpoint="/admin/students" fields={STUDENT_FIELDS} columns={STUDENT_COLUMNS} />}
              />
              <Route
                path="faculty"
                element={<ManagePage title="Faculty" noun="faculty member" endpoint="/admin/faculty" fields={FACULTY_FIELDS} columns={FACULTY_COLUMNS} />}
              />
              <Route path="courses" element={<Courses />} />
              <Route path="courses/:id" element={<CourseDetail />} />
              <Route
                path="reports"
                element={<CourseReport loadCourses={() => api.get('/admin/courses?size=100').then((p) => p.content)} />}
              />
            </Route>
            <Route path="faculty" element={<RoleGate role="FACULTY" />}>
              <Route index element={<FacultyHome />} />
              <Route path="mark" element={<MarkAttendance />} />
              <Route path="reports" element={<CourseReport loadCourses={() => api.get('/faculty/me/courses')} />} />
            </Route>
            <Route path="student" element={<RoleGate role="STUDENT" />}>
              <Route index element={<StudentHome />} />
              <Route path="history" element={<StudentHistory />} />
              <Route path="profile" element={<StudentProfile />} />
            </Route>
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </HashRouter>
  );
}
