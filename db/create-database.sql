-- job-monitor: creates the app's database login and its two databases.
-- Run it once, from a Command Prompt, as the PostgreSQL admin user:
--
--   "C:\Program Files\PostgreSQL\18\bin\psql.exe" -h localhost -U postgres -f D:\job-monitor\db\create-database.sql
--
-- psql first asks for the postgres password (yours; it stays on your machine).
-- Then it asks twice for a NEW password for the app's login. Typing is hidden.
-- Put that same new password in the JOB_MONITOR_DB_PASSWORD environment variable:
--
--   setx JOB_MONITOR_DB_PASSWORD "the-new-password"
--
-- (setx affects Command Prompt windows opened afterwards, not the current one.)

CREATE ROLE job_monitor_app LOGIN;
\password job_monitor_app

-- The app's data.
CREATE DATABASE job_monitor OWNER job_monitor_app;

-- Used only by the automated tests, so they never touch your real data.
CREATE DATABASE job_monitor_test OWNER job_monitor_app;
