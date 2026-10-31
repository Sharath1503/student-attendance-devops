# Verification status (what was actually run vs. what was only written)

Created in a sandbox with JDK 21, Maven 3.9, Node 22, Python and Git. **Maven Central was blocked by the sandbox network policy**, there was no
MySQL, no Jenkins, no Docker daemon and no remote Linux server. Nothing below is claimed unless it was really executed.

| Area | What was done | Result |
|---|---|---|
| Frontend build | `npm install`, `npm run build` | ✅ passed (49 modules, Vite 5) |
| Frontend tests | `npm test` (Vitest) | ✅ 7 / 7 passed |
| Attendance % logic | `AttendanceCalculator.java` compiled with plain `javac` and run against 9 checks (2/3, 1/3, 3/4, 8/8, 0/8, no data, low flag) | ✅ 9 / 9 passed |
| Java syntax | `javac` over all 71 backend files (main + test) | ✅ no syntax errors; ❌ **full compile impossible** – Spring/JWT/Hibernate jars could not be downloaded, so type errors against the real APIs are **not ruled out** |
| Backend tests | 26 unit + 31 integration tests written | ❌ **never executed** |
| `mvn clean verify`, `mvn package` | — | ❌ not run |
| Backend startup against MySQL | — | ❌ not run (SQL was written to be portable MySQL 8 / H2) |
| Ansible | `ansible-playbook --syntax-check` on `deploy.yml` and `rollback.yml`; `ansible-lint` (remaining findings are style rules, relaxed in `ansible/.ansible-lint`) | ✅ syntax OK; ❌ not executed against a server (no systemd host) |
| Jenkinsfile | Written to declarative syntax | ❌ not run in Jenkins, not linted |
| GitHub push / webhook | — | ❌ not done (no access) |
| Deployment, health check, rollback | — | ❌ not performed |
| Secret scan | `git grep` for passwords/keys/tokens across the repo | ✅ see below |

## What this means for you
* Expect the **first `mvn clean verify` to possibly show a few compile or test failures** – that is normal for code written without a compiler. Send me the first error and it will be fixed quickly.
* The frontend, SQL schema, Ansible role and Jenkinsfile are the parts least likely to need changes; the Java is the part most likely to need small fixes.
* Items marked “not run” in this table are exactly the steps you must demonstrate yourself for your report (use `TEST_REPORT_TEMPLATE.md`).

## Secret scan
Run before every push: `git grep -n -i -E "(password|secret|token|apikey|private key)" -- . ':!docs' ':!*.md'` – every hit should be a
variable name, a placeholder (`CHANGE_ME…`) or a clearly labelled throw-away test constant (`backend/src/test/resources/application-test.properties`).
