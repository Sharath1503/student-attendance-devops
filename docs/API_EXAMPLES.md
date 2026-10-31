# API examples

Interactive docs: `http://localhost:8080/swagger-ui.html` (click **Authorize**, paste the token). All errors share one JSON shape.

## Login
```bash
curl -s -X POST http://localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"faculty1","password":"<your demo password>"}'
```
```json
{ "token": "eyJhbGciOi…", "tokenType": "Bearer", "expiresInSeconds": 3600,
  "user": { "id": 3, "username": "faculty1", "role": "FACULTY", "displayName": "Dr. Anita Rao" } }
```
```bash
TOKEN=eyJhbGciOi…
curl -s http://localhost:8080/api/faculty/me/courses -H "Authorization: Bearer $TOKEN"
```

## Error shape
```json
{ "timestamp": "2026-03-10T10:00:00Z", "status": 400, "error": "Bad Request", "message": "Validation failed",
  "path": "/api/admin/students", "fieldErrors": { "email": "Email is not valid" } }
```
401 = missing/invalid/expired token or bad credentials · 403 = role/course not permitted · 404 · 409 = duplicate/conflict · 400 = validation.

## Endpoints
| Method & path | Role | Purpose |
|---|---|---|
| POST `/api/auth/login` | public | get token |
| GET `/api/auth/me` · POST `/api/auth/logout` | any | current user · audit logout |
| GET/POST `/api/admin/students`, GET/PUT/DELETE `/api/admin/students/{id}` | ADMIN | list (`q,page,size`) / create / read / update / deactivate |
| same for `/api/admin/faculty` | ADMIN | faculty accounts |
| GET/POST `/api/admin/courses`, GET/PUT/DELETE `/api/admin/courses/{id}` | ADMIN | courses (GET one includes faculty + students) |
| POST/DELETE `/api/admin/courses/{id}/faculty/{facultyId}` | ADMIN | assign / unassign faculty |
| POST/DELETE `/api/admin/courses/{id}/students/{studentId}` | ADMIN | enrol / unenrol |
| GET `/api/admin/dashboard` · GET `/api/admin/audit` | ADMIN | statistics · audit log |
| GET `/api/faculty/me`, `/api/faculty/me/courses` | FACULTY | profile · assigned courses |
| GET `/api/attendance/courses/{id}/students` | ADMIN, assigned FACULTY | roster |
| POST `/api/attendance/courses/{id}/sessions` | assigned FACULTY | create/update a class |
| GET `/api/attendance/courses/{id}/sessions` · `…/sessions/{sid}` · `…/summary` | ADMIN, assigned FACULTY | history · one class · per-student summary |
| GET `/api/student/profile` · `/courses` · `/attendance?courseId=` · `/summary` | STUDENT | own data only |
| GET `/actuator/health` (+ `/liveness`, `/readiness`) | public | health |

## Record attendance
```bash
curl -s -X POST http://localhost:8080/api/attendance/courses/1/sessions \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"date":"2026-03-10","slot":1,"status":"HELD","records":[{"studentId":1,"status":"PRESENT"},{"studentId":2,"status":"ABSENT"}]}'
```
```json
{ "session": { "id": 7, "courseId": 1, "courseCode": "MCA501", "date": "2026-03-10", "slot": 1, "status": "HELD", "presentCount": 1, "absentCount": 1 },
  "records": [ { "studentId": 1, "studentCode": "MCA24001", "studentName": "Ravi Kumar", "status": "PRESENT" },
               { "studentId": 2, "studentCode": "MCA24002", "studentName": "Sneha Patel", "status": "ABSENT" } ] }
```
Mark a class cancelled: `{"date":"2026-03-11","slot":1,"status":"CANCELLED"}` (no `records`).

## Student summary
```json
{ "overallPercentage": 66.67, "overallPresent": 2, "overallTotal": 3, "lowThresholdPercent": 75.0,
  "courses": [ { "courseId": 1, "courseCode": "MCA501", "courseName": "DevOps…", "present": 2, "absent": 1, "total": 3, "percentage": 66.67, "low": true } ],
  "warnings": [ "Your overall attendance is 66.67%, below the required 75.0%", "Low attendance in MCA501 - DevOps…: 66.67% (minimum 75.0%)" ] }
```
