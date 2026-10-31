# Quick Start – run it on your computer (≈15 minutes)

> **Read this first – honest status.** The React frontend, Ansible syntax and the percentage logic were built/tested while
> creating this project. The **Java backend was written but could not be compiled in the environment that created it**
> (Maven Central was blocked there), and **Jenkins / a real deployment were never run**. So the very first
> `mvn verify` on your machine is the real test. If something fails, see [“If the first build fails”](#if-the-first-build-fails).
> Full table: [docs/VERIFICATION_STATUS.md](docs/VERIFICATION_STATUS.md).

## 1. Install these once

| Tool | Version | Check with |
|---|---|---|
| JDK | **21** | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| Node.js | 20 or 22 | `node --version` |
| MySQL Server | 8.x | `mysql --version` |
| Git | any | `git --version` |

Windows: use **Git Bash** (comes with Git) or **WSL2 Ubuntu** to run the `.sh` scripts. Everything else works in PowerShell too.

## 2. Create the database (one time)

1. Open `scripts/mysql-setup.sql` and change `CHANGE_ME_db_password` to a password you choose (don't commit that edit).
2. Run: `mysql -u root -p < scripts/mysql-setup.sql`

## 3. Create your settings file

```bash
cp .env.example .env
```
Edit `.env` and replace every `CHANGE_ME…` value:

* `DB_PASSWORD` – the password you chose in step 2
* `JWT_SECRET` – any random text of 32+ characters (`openssl rand -base64 48`)
* `BOOTSTRAP_ADMIN_PASSWORD` – the password for your first admin login (8+ chars)
* For a demo with ready-made faculty/students: set `SEED_DEMO_DATA=true` and `SEED_DEMO_PASSWORD=<8+ chars>`

## 4. Build (runs all tests) and start

```bash
./scripts/build-all.sh        # UI build + Maven "verify" (unit + integration tests) → backend/target/student-attendance.jar
./scripts/run-local.sh        # starts the app using .env
```
Open **http://localhost:8080** and log in:

| Role | Username | Password |
|---|---|---|
| Admin | `admin` (or your `BOOTSTRAP_ADMIN_USERNAME`) | your `BOOTSTRAP_ADMIN_PASSWORD` |
| Faculty (demo data only) | `faculty1` | your `SEED_DEMO_PASSWORD` |
| Students (demo data only) | `student1`, `student2`, `student3` | your `SEED_DEMO_PASSWORD` |

Other URLs: API docs `http://localhost:8080/swagger-ui.html` · health `http://localhost:8080/actuator/health`

**Windows PowerShell alternative to step 4** (no bash):
```powershell
cd frontend; npm ci; npm run build; cd ..
Remove-Item -Recurse -Force backend\src\main\resources\static -ErrorAction SilentlyContinue
New-Item -ItemType Directory backend\src\main\resources\static | Out-Null
Copy-Item -Recurse frontend\dist\* backend\src\main\resources\static\
cd backend; mvn clean verify; cd ..
# set the variables from .env in this window, e.g.  $env:DB_PASSWORD="..."; $env:JWT_SECRET="..."; $env:BOOTSTRAP_ADMIN_PASSWORD="..."
java -jar backend\target\student-attendance.jar
```

## If the first build fails

1. Copy the **first** `[ERROR]` block (or the failing test name + message) from the Maven output.
2. Common causes: wrong Java version (must be 21), no internet for Maven, MySQL not running / wrong `DB_PASSWORD` when *starting* the app (tests use an in-memory database and don’t need MySQL).
3. Send me that error text and I’ll fix the code. To just get the app running meanwhile: `./scripts/build-all.sh -DskipTests`
   (for demos only – the pipeline never skips tests).

## 5. Optional: front-end development mode

Terminal 1: start the backend (step 4). Terminal 2: `cd frontend && npm install && npm run dev` → http://localhost:5173

## 6. Optional: the full GitHub → Jenkins → Maven → Ansible demo

Needs a GitHub account, a Jenkins server and a Linux VM/server. Step-by-step with exact commands:
**[docs/DEVOPS_SETUP_GUIDE.md](docs/DEVOPS_SETUP_GUIDE.md)**. Without a server you can still test the Ansible role against your own
Ubuntu machine (`ansible/inventory.local.ini.example`).

## Checklist of what only YOU can do

- [ ] Install JDK 21, Maven, Node, MySQL (step 1)
- [ ] Create the MySQL database/user with your own password (step 2)
- [ ] Fill in `.env` (step 3) – never commit it (it is git-ignored)
- [ ] Run the build and report any failure (step 4)
- [ ] Create your GitHub repository and push (README §13)
- [ ] Install Jenkins + plugins, add the 5 credentials, create the pipeline job, add the GitHub webhook (DevOps guide)
- [ ] Prepare a Linux server: deploy user, SSH key, MySQL, `known_hosts` entry (DevOps guide)
- [ ] Run the pipeline and capture screenshots/logs for your report
