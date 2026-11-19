# Starts the app from a PowerShell window, reading the database password
# saved by db/setup-database.ps1 even if this window was opened before it.
if (-not $env:JOB_MONITOR_DB_PASSWORD) {
    $env:JOB_MONITOR_DB_PASSWORD = [Environment]::GetEnvironmentVariable('JOB_MONITOR_DB_PASSWORD', 'User')
}
Set-Location $PSScriptRoot
& .\mvnw.cmd -q spring-boot:run
