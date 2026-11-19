# job-monitor

A local app that sends job applications from Gmail, one at a time, and tracks replies.
It runs only on this computer: it answers at http://127.0.0.1:8080 and has no login.

**Status:** phase 1 (Master data). Mail goes to a built-in fake mail server, so nothing leaves this computer. Gmail is connected in phase 3.

## First-time setup

1. **Create the database.** In a Command Prompt, run:

   ```
   "C:\Program Files\PostgreSQL\18\bin\psql.exe" -h localhost -U postgres -f D:\job-monitor\db\create-database.sql
   ```

   It asks for your postgres password, then twice for a new password for the app's login (typing is hidden).

2. **Save the app's database password** (the new one from step 1):

   ```
   setx JOB_MONITOR_DB_PASSWORD "the-new-password"
   ```

   Open a new Command Prompt afterwards; `setx` doesn't change windows that are already open.

3. **Start the app:** double-click `start.bat`. The browser opens when it's ready. The first start creates the tables.

## What works now

Master data: profile, Gmail account (signs in to the fake mail server), resumes, templates with a live preview, sending settings, days off, free-mail domains and job-alert senders. The dashboard shows a setup checklist.

## Where things are kept

- **Data:** PostgreSQL database `job_monitor`, login `job_monitor_app`.
- **Encryption key:** `%USERPROFILE%\.job-monitor\secret.key`, created on first start. Mailbox passwords are encrypted with it. **Back it up**; without it, saved passwords can't be read.
- **Old recruiters to import in phase 2:** `data/import/old-recruiters.txt` (544 unique addresses).

## Tests

```
mvnw.cmd test
```

Tests never send real email. The database tests use `job_monitor_test` and skip themselves until step 1 and 2 above are done.
