-- Used by setup-database.ps1. Safe to run more than once.
-- :'app_pw' is passed in by the script; it's never written to disk.

SELECT 'CREATE ROLE job_monitor_app LOGIN'
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'job_monitor_app')\gexec

ALTER ROLE job_monitor_app LOGIN PASSWORD :'app_pw';

SELECT 'CREATE DATABASE job_monitor OWNER job_monitor_app'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'job_monitor')\gexec

SELECT 'CREATE DATABASE job_monitor_test OWNER job_monitor_app'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'job_monitor_test')\gexec

\echo
\echo Done: login job_monitor_app, databases job_monitor and job_monitor_test.
