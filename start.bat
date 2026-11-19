@echo off
rem Starts job-monitor and opens it in your browser when it's ready.
rem Stop it with Ctrl+C in this window.
cd /d "%~dp0"

if "%JOB_MONITOR_DB_PASSWORD%"=="" (
  echo.
  echo JOB_MONITOR_DB_PASSWORD isn't set, so the app can't reach its database.
  echo See README.md, "First-time setup".
  echo.
  pause
  exit /b 1
)

call mvnw.cmd -q spring-boot:run -Dspring-boot.run.arguments=--jobmonitor.open-browser=true
