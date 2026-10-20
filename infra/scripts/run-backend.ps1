# CityGo backend dev runner — loads root .env, sets JAVA_HOME, boots Spring Boot (dev profile).
param(
    [string]$Profile = "dev",
    [switch]$SkipBuild
)
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
$backend = Join-Path $root "backend"
Set-Location $backend
if (-not $SkipBuild) {
    .\mvnw.cmd -q -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw "maven package failed" }
}
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=$Profile"
