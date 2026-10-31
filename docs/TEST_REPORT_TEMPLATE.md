# Test report

Project: Online Student Attendance System · Build #: ______ · Commit: ______ · Date: ______ · Tester: ______

## 1. Automated tests (copy from Maven / Jenkins “Test Result”)
| Suite | Command | Tests | Passed | Failed | Skipped | Result |
|---|---|---|---|---|---|---|
| Backend unit (`*Test`) | `mvn test` | 26 expected | | | | |
| Backend integration (`*IT`) | `mvn verify` | 31 expected | | | | |
| Frontend unit | `npm test` | 7 expected | | | | |

## 2. Pipeline run
| Stage | Status | Duration | Notes |
|---|---|---|---|
| Checkout | | | |
| Verify tools | | | |
| Build frontend | | | |
| Build, test and package | | | |
| Archive artifact | | | |
| Deployment gate | | | |
| Deploy with Ansible | | | |
| Verify deployment | | | |

## 3. Manual acceptance tests
| ID | Steps | Expected | Actual | Pass/Fail |
|---|---|---|---|---|
| AC-01 | Log in as admin, faculty, student; try a wrong password | Correct dashboard per role; wrong password rejected | | |
| AC-02 | Admin: add/edit/deactivate a student and a faculty; add a course; assign faculty; enrol student | Saved; duplicates rejected | | |
| AC-03 | Faculty: mark a class for an assigned course; try again for another faculty's course | Saved; second is refused (403) | | |
| AC-04 | Student: view history, per-course %, overall %, warning when below 75% | Figures match the rules in ATTENDANCE_RULES.md | | |
| AC-05 | Push a commit to GitHub | Jenkins build starts automatically | | |
| AC-06 | Check the build output | JAR created | | |
| AC-07 | Open Jenkins “Test Result” | Results visible | | |
| AC-08 | Break a test, push | Build fails, deploy stages skipped | | |
| AC-09 | Run the deploy stage | Server configured, JAR deployed | | |
| AC-10 | `curl <server>:8080/actuator/health` | `{"status":"UP"}` | | |
| AC-11 | Look at the final build status/console | Status and errors shown | | |
| AC-12 | `git grep -i -E 'password|secret'` and review | No real credentials in the repository | | |

## 4. Defects found
| # | Description | Severity | Status |
|---|---|---|---|

## 5. Conclusion
