# CityGo frontend dev runner — installs deps if needed, starts vite dev server.
param([int]$Port=5173)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$frontend = Join-Path $root "frontend"
Set-Location $frontend
if (-not (Test-Path (Join-Path $frontend "node_modules"))) {
    Write-Host "==> npm install (first run) ..."
    npm install
    if ($LASTEXITCODE -ne 0) { throw "npm install failed" }
}
npm run dev -- --host 127.0.0.1 --port $Port --strictPort
