# CityGo backend test runner — loads root .env, runs mvnw test (test profile → citygo_test).
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$envFile = Join-Path $root ".env"
if (Test-Path $envFile) {
    Get-Content $envFile | Where-Object { $_ -match '^\s*[A-Za-z_][A-Za-z0-9_]*=' } | ForEach-Object {
        $k, $v = $_ -split '=', 2
        $k = $k.Trim(); $v = $v.Trim()
        if (-not [Environment]::GetEnvironmentVariable($k, "Process")) {
            [Environment]::SetEnvironmentVariable($k, $v, "Process")
        }
    }
}
. (Join-Path $PSScriptRoot 'use-java21.ps1')
Set-Location (Join-Path $root "backend")
.\mvnw.cmd test
exit $LASTEXITCODE
