# Creates job-monitor's database login and databases.
# The app's password is generated here and saved in your user environment
# (JOB_MONITOR_DB_PASSWORD). It's never shown. You only type the postgres
# admin password when psql asks for it.

$ErrorActionPreference = 'Stop'
$psql = 'C:\Program Files\PostgreSQL\18\bin\psql.exe'

$appPassword = [Environment]::GetEnvironmentVariable('JOB_MONITOR_DB_PASSWORD', 'User')
if (-not $appPassword) {
    $bytes = New-Object byte[] 24
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    $appPassword = [Convert]::ToBase64String($bytes).Replace('+', 'A').Replace('/', 'B').Replace('=', '')
    [Environment]::SetEnvironmentVariable('JOB_MONITOR_DB_PASSWORD', $appPassword, 'User')
    Write-Host 'Generated a password for the app and saved it as JOB_MONITOR_DB_PASSWORD.'
}

Write-Host ''
Write-Host 'psql will now ask for the postgres (admin) password.'
& $psql -h localhost -U postgres -v ON_ERROR_STOP=1 -v "app_pw=$appPassword" -f "$PSScriptRoot\setup-database.sql"
if ($LASTEXITCODE -ne 0) { Write-Host 'Setup failed. Check the message above.'; exit 1 }
