# Requirements traceability matrix

Paths are relative to the repository root. **Status**: ✅ checked in the build environment · 🟡 implemented, must be confirmed on your machine/servers (see [VERIFICATION_STATUS.md](VERIFICATION_STATUS.md)).
`B/…` = `backend/src/main/java/com/attendance/…`, `T/…` = `backend/src/test/java/com/attendance/…`.

## Functional requirements
| Req | Implementation | Files | Tests | How to verify | Status |
|---|---|---|---|---|---|
| FR-01 Authentication, roles | JWT login, BCrypt, role rules | `B/security/*`, `B/config/SecurityConfig.java`, `B/service/AuthService.java`, `frontend/src/auth.jsx`, `pages/Login.jsx` | `T/it/AuthIT`, `T/security/JwtServiceTest` | Log in as each role; wrong password → 401; wrong role → 403 | 🟡 |
| FR-02 Student management | CRUD + search + paging, duplicate checks, deactivate | `B/service/StudentService.java`, `B/controller/AdminStudentController.java`, `pages/admin/ManagePage.jsx` | `T/it/AdminApiIT`, `T/dto/DtoValidationTest` | Admin → Students | 🟡 |
| FR-03 Faculty management & course association | Faculty CRUD, assign/unassign to courses | `B/service/FacultyService.java`, `CourseService.java`, `AdminFacultyController`, `AdminCourseController`, `pages/admin/CourseDetail.jsx` | `T/it/AdminApiIT` | Admin → Courses → Faculty & students | 🟡 |
| FR-04 Attendance recording | Session + record per student, rules in service, DB unique keys | `B/service/AttendanceService.java`, `AttendanceController`, `V1__init_schema.sql`, `pages/faculty/MarkAttendance.jsx` | `T/service/AttendanceServiceTest`, `T/it/AttendanceFlowIT`, `T/it/DatabaseConstraintIT` | Faculty → Mark attendance | 🟡 |
| FR-05 Viewing & reporting | Student portal, course summary, sessions, dashboard | `B/service/StudentPortalService.java`, `AttendanceService`, `AttendanceCalculator`, `DashboardService`, `pages/student/*`, `pages/CourseReport.jsx` | `AttendanceCalculatorTest` ✅, `AttendanceFlowIT` | Student dashboard; Reports page | calculator ✅ / rest 🟡 |
| FR-06 Source control | Git repo, `.gitignore`, docs | `.gitignore`, `README.md §13` | — | `git log`, push to GitHub | 🟡 |
| FR-07 Automated build | Maven lifecycle, surefire + failsafe | `backend/pom.xml` | all tests | `mvn clean verify` | 🟡 |
| FR-08 Continuous integration | `githubPush()` trigger (+ polling alternative) | `Jenkinsfile`, `docs/DEVOPS_SETUP_GUIDE.md §9` | — | Push a commit | 🟡 (not run) |
| FR-09 Automated testing & reports | JUnit step publishes surefire + failsafe XML; failure stops pipeline | `Jenkinsfile` (stage “Build, test and package”) | — | Jenkins “Test Result”; break a test | 🟡 (not run) |
| FR-10 Packaging | Executable Spring Boot JAR incl. UI, archived by Jenkins | `backend/pom.xml`, `Jenkinsfile`, `scripts/build-all.sh` | — | `backend/target/student-attendance.jar` | 🟡 |
| FR-11 Server configuration | Ansible role: Java, user, dirs, env file, systemd unit | `ansible/roles/attendance_app/**` | — | Run playbook (guide §10) | syntax ✅ / run 🟡 |
| FR-12 Deployment | Versioned release + symlink + restart | `ansible/playbooks/deploy.yml`, `roles/…/tasks/main.yml` | — | same | syntax ✅ / run 🟡 |
| FR-13 Pipeline monitoring | Named stages, `timestamps`, messages in `post` | `Jenkinsfile` | — | Jenkins stage view | 🟡 (not run) |
| FR-14 Deployment verification | Ansible `uri` health check with retries, rollback on failure; optional Jenkins curl | `roles/…/tasks/main.yml`, `Jenkinsfile`, `application.properties` (actuator) | `AuthIT.healthIsPublicAndMinimal` | Deploy; `curl …/actuator/health` | 🟡 |
| FR-15 Credential security | env vars, Jenkins credentials, 0600 env file, `no_log`, git-ignore | `.env.example`, `.gitignore`, `Jenkinsfile`, `attendance.env.j2` | secret scan (see status doc) ✅ | `git grep`, review | ✅ scan / 🟡 runtime |

## Non-functional requirements
| Req | How it is met | Where |
|---|---|---|
| NFR-01 Reliability | Ordered declarative stages; failure stops later stages; deploy rollback | `Jenkinsfile`, `tasks/main.yml` |
| NFR-02 Maintainability | Layered backend, role-based Ansible, docs | repository layout |
| NFR-03 Performance | Indexed queries, aggregate SQL, paging; stages skipped for non-deploy branches | `V1__init_schema.sql`, repositories |
| NFR-04 Security | See README §21 | `SecurityConfig`, services, Ansible |
| NFR-05 Scalability | Stateless JWT app (multi-server ready), inventory groups | `SecurityConfig`, `ansible/` |
| NFR-06 Availability | `Restart=on-failure` systemd unit, health endpoint | `attendance.service.j2` |
| NFR-07 Usability | Role dashboards, validation, empty/loading/error states, responsive CSS | `frontend/src` |
| NFR-08 Consistency | Idempotent tasks, versioned releases | `tasks/main.yml` |

## Acceptance criteria
| ID | Where it is demonstrated | Evidence to collect |
|---|---|---|
| AC-01 | `AuthIT`, Login page | screenshots of 3 role logins |
| AC-02 | `AdminApiIT`, Admin pages | screenshots, test report |
| AC-03 | `AttendanceFlowIT` (assigned vs unassigned faculty) | test report |
| AC-04 | `AttendanceFlowIT.studentSeesCorrectPercentages`, Student dashboard | screenshot |
| AC-05 | `Jenkinsfile` `githubPush()` + webhook | Jenkins “Started by GitHub push” |
| AC-06 | `mvn clean verify` | JAR in `backend/target` |
| AC-07 | `junit` step | Jenkins Test Result page |
| AC-08 | Guide §11.1 | build with failed stage and skipped deploy |
| AC-09 | `ansible/…` | Ansible log “Deployment OK” |
| AC-10 | Health check task | `curl` output `{"status":"UP"}` |
| AC-11 | Jenkins `post {}` blocks, console | final build status |
| AC-12 | `.gitignore`, `.env.example`, Jenkins credentials | `git grep` result |
