# DevOps Setup Guide – GitHub → Jenkins → Maven → Ansible

Commands assume **Ubuntu 22.04/24.04** for Jenkins and for the target server. They can be the same machine for a demo, but two machines
(or one VM for Jenkins and your laptop/another VM as target) shows the pipeline better.
Package repositories and key URLs change over time – if a repository command fails, copy the current one from
<https://www.jenkins.io/doc/book/installing/linux/>.

## 0. What is verified and what is not
Ansible playbook syntax and the Jenkinsfile structure were prepared here; **no Jenkins server or target server was available, so the pipeline and deployment have not been executed.** Run §8 and report problems.

## 1. Push the project to GitHub
See README §13. Use a **private** repo if you prefer. Never commit `.env`, keys or `ansible/inventory.ini` (all git-ignored).

## 2. Install Jenkins
```bash
sudo apt update && sudo apt install -y fontconfig openjdk-21-jre git curl
# add the official Jenkins repository (current instructions: jenkins.io → Installing → Linux → Debian/Ubuntu), then:
sudo apt update && sudo apt install -y jenkins
sudo systemctl enable --now jenkins
sudo cat /var/lib/jenkins/secrets/initialAdminPassword      # use this on first visit to http://<server>:8080
```
Choose “Install suggested plugins”, create an admin user. (If Jenkins and the app share one machine, change one of them to another port, e.g. app `SERVER_PORT=8081`.)

**Plugins** (Manage Jenkins → Plugins): *Pipeline, Git, GitHub, GitHub Branch Source, NodeJS, Credentials Binding, JUnit, Timestamper* (most are in “suggested”).

## 3. Tools on the Jenkins machine
```bash
sudo apt install -y openjdk-21-jdk maven ansible
sudo -u jenkins ansible-playbook --version
```
Manage Jenkins → **Tools** → add exactly these names (the Jenkinsfile uses them):
* JDK → name `jdk21` → JAVA_HOME `/usr/lib/jvm/java-21-openjdk-amd64`
* Maven → name `maven3` → “Install automatically” or point to `/usr/share/maven`
* NodeJS → name `node22` → install automatically, version 22.x

## 4. Prepare the target server
On the **target** (as a sudo-capable admin):
```bash
sudo adduser --disabled-password --gecos "" deploy
echo 'deploy ALL=(ALL) NOPASSWD:ALL' | sudo tee /etc/sudoers.d/deploy && sudo chmod 440 /etc/sudoers.d/deploy
sudo apt install -y mysql-server
sudo mysql < scripts/mysql-setup.sql          # after editing the password placeholder
```
(Passwordless full sudo is a demo simplification – narrow it for real use.) Ansible installs Java and everything else.

## 5. SSH key for deployment
On the **Jenkins** machine:
```bash
ssh-keygen -t ed25519 -f ~/attendance_deploy_key -C "jenkins-deploy" -N ""
ssh-copy-id -i ~/attendance_deploy_key.pub deploy@<TARGET_IP>
ssh -i ~/attendance_deploy_key deploy@<TARGET_IP> 'echo ok'
```
The private key content goes into a Jenkins credential (§7) – never into Git.

## 6. Trust the server’s host key (keeps host-key checking ON)
```bash
sudo -u jenkins mkdir -p /var/lib/jenkins/.ssh
ssh-keyscan -H <TARGET_IP> | sudo -u jenkins tee -a /var/lib/jenkins/.ssh/known_hosts
```
Verify the fingerprint matches the server (`ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub` on the target).

## 7. Jenkins credentials (Manage Jenkins → Credentials → Global)
| ID (exact) | Kind | Value |
|---|---|---|
| `attendance-deploy-ssh` | SSH Username with private key | username `deploy`, paste private key |
| `attendance-inventory` | Secret file | your `inventory.ini` (copy `ansible/inventory.ini.example`, set the real IP) |
| `attendance-db-password` | Secret text | MySQL password of `attendance_app` |
| `attendance-jwt-secret` | Secret text | 32+ random chars (`openssl rand -base64 48`) |
| `attendance-admin-password` | Secret text | first admin password (8+ chars) |

Optional demo data on the server: set `seed_demo_data: "true"` in `ansible/group_vars/attendance.yml` **and** export `SEED_DEMO_PASSWORD` (only for a throw-away demo server).
If the database is not on the app server, change `db_url` in `ansible/group_vars/attendance.yml`.

## 8. Create the pipeline job
**Recommended – Multibranch Pipeline:** New Item → *Multibranch Pipeline* → Branch Sources → GitHub (add a GitHub credential for private repos) → repository URL → Build Configuration “by Jenkinsfile” (path `Jenkinsfile`) → Save.
Feature branches are built/tested; only `main` (parameter `DEPLOY_BRANCH`) is deployed.
**Alternative – single Pipeline job:** *Pipeline script from SCM* → Git → branch `*/main` → script path `Jenkinsfile`.

First run: *Build with Parameters*. Watch the stage view: Checkout → Verify tools → Build frontend → Build, test and package → Archive → Deployment gate → Deploy with Ansible → (Verify deployment).
Results: **Test Result** link on the build (JUnit), **Artifacts** shows `student-attendance.jar`, console log shows the Ansible output (secrets are masked).
To build without deploying: untick `DEPLOY`. Set `APP_HEALTH_URL=http://<TARGET_IP>:8080/actuator/health` for the extra check from Jenkins.

## 9. GitHub webhook (trigger on push)
GitHub repo → Settings → Webhooks → Add: Payload URL `http://<JENKINS_HOST>:8080/github-webhook/`, content type `application/json`, event “Just the push event”.
Jenkins must be reachable from the internet (public IP, or a tunnel such as ngrok for a laptop). If it is not, use polling instead: in the Jenkinsfile replace
`githubPush()` with `pollSCM('H/5 * * * *')` (PRD allows polling). For multibranch jobs also enable “Scan by webhook” or periodic scans.

## 10. Test Ansible without Jenkins (recommended before the pipeline)
```bash
cd student-attendance-devops
./scripts/build-all.sh
cp ansible/inventory.ini.example ansible/inventory.ini     # edit IP/user
export DB_PASSWORD='…' JWT_SECRET='…' BOOTSTRAP_ADMIN_PASSWORD='…'
cd ansible
ansible-playbook -i inventory.ini playbooks/deploy.yml --private-key ~/attendance_deploy_key \
   -e app_jar_src=../backend/target/student-attendance.jar -e app_version=manual-1
```
**No remote server?** Use your own Ubuntu machine/VM with systemd: `cp ansible/inventory.local.ini.example ansible/inventory.local.ini`, then run the same command with `-i inventory.local.ini` (no `--private-key`, add `-K` if sudo asks a password). Windows: WSL2 with systemd enabled.
Repeat the command: nothing should change except the restart (idempotent). Rollback by hand: `ansible-playbook -i inventory.ini playbooks/rollback.yml`.

## 11. Demonstrating the failure scenarios (for your report)
1. **Failed test blocks deployment:** in `AttendanceCalculator.percentage` change `* 10000.0` to `* 9000.0`, push to `main` → stage *Build, test and package* turns red, JUnit report lists the failing tests, **Deploy stages are skipped** (not run). Revert and push.
2. **Failed deployment is detected and rolled back:** on the server edit nothing – instead run the pipeline with a wrong `attendance-db-password` credential → app can’t start → Ansible health check fails → playbook prints the last log lines, switches `current.jar` back to the previous release, and the stage fails.
3. **Feature branches never deploy:** push a branch `feature/x` → built and tested, “Deployment gate” skipped.

## 12. Server layout and operations (what Ansible creates)
```
/opt/attendance/releases/attendance-<build>-<commit>.jar   (newest 5 kept)
/opt/attendance/current.jar  → symlink to the live release
/etc/attendance/attendance.env                              (root-only 0600: DB/JWT secrets)
/etc/systemd/system/attendance.service
```
```bash
systemctl status attendance        journalctl -u attendance -f        curl -s localhost:8080/actuator/health
```
Tip: put nginx or Caddy in front for HTTPS (not included).

## 13. Troubleshooting the pipeline
| Problem | Fix |
|---|---|
| `jdk21`/`maven3`/`node22` not found | Create tools with those exact names (§3) |
| `ansible-playbook: not found` | `sudo apt install ansible` on the Jenkins machine |
| `Host key verification failed` | §6 |
| `Permission denied (publickey)` | §5; check the credential holds the **private** key and the user is `deploy` |
| `sudo: a password is required` | §4 sudoers file |
| Assertion “Missing input…” | One of the three secret credentials is missing/empty, or `JWT_SECRET` < 32 chars |
| Webhook shows red ✗ in GitHub | Jenkins not reachable from GitHub – use ngrok or polling (§9) |
| Build works locally, fails on Jenkins | Compare `java -version`/`node --version` in the “Verify tools” stage |
| App up but health check fails | `journalctl -u attendance -n 100` – usually wrong `db_url` or DB password |
