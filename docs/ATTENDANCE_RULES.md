# Attendance rules (as implemented)

| Topic | Rule |
|---|---|
| Class identity | A class session = (course, date, slot). Slot 1–10 allows several classes per day. Unique in the database. |
| Session status | `HELD` (counts), `CANCELLED`, `HOLIDAY` (stored, never counted). |
| Student record | One `PRESENT`/`ABSENT` per (session, student). Unique in the database; re-submitting a class **updates** it. |
| Percentage | `present ÷ (present + absent) × 100`, rounded to 2 decimals, over **HELD** sessions that have a record for that student. |
| Cancelled / holiday | Not counted. Changing a held class to cancelled/holiday deletes its student records. |
| Unrecorded sessions | A student with no record for a session (e.g. enrolled later) is not counted for it. A session that was never created is not counted at all. |
| No data | No counted classes → percentage is **N/A** (null), not 0 or 100, and no warning. |
| Low attendance | Percentage strictly below `ATTENDANCE_LOW_THRESHOLD` (default 75) → flagged; student sees warnings; admin dashboard counts such students. |
| Who may record | Only FACULTY assigned to the course (checked on the server). Admins and students cannot. |
| Who may view | Admin: every course. Faculty: assigned courses. Student: own data only (no id parameter exists). |
| Dates | No future dates. New or changed classes only within `ATTENDANCE_EDIT_WINDOW_DAYS` (default 7) of the class date. |
| Roster | Every student sent must be enrolled in the course; the same student twice in one request is rejected. |
| Deleting people/courses | “Delete” = deactivate (login disabled, history kept). Inactive courses accept no new attendance. |
