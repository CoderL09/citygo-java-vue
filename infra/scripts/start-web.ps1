# Serve the built frontend through a localhost-only Nginx container.
param([int]$BackendPort=8083,[switch]$SkipBuild)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
if(-not $SkipBuild){Push-Location (Join-Path $projectRoot 'frontend');try{npm run build;if($LASTEXITCODE -ne 0){throw 'Frontend build failed'}}finally{Pop-Location}}
if(-not (Test-Path -LiteralPath (Join-Path $projectRoot 'frontend/dist/index.html'))){throw 'Frontend dist/index.html is missing; build first.'}
$health=Invoke-RestMethod "http://127.0.0.1:$BackendPort/actuator/health"
if($health.status -ne 'UP'){throw 'Backend must be healthy before starting the local Nginx entry.'}
$env:CITYGO_NGINX_BACKEND="host.docker.internal:$BackendPort"
Push-Location $projectRoot
try{docker compose --env-file .env -f infra/docker-compose.yml --profile web up -d web;if($LASTEXITCODE -ne 0){throw 'Nginx startup failed'};docker exec citygo-web nginx -t;if($LASTEXITCODE -ne 0){throw 'Nginx syntax check failed'}}finally{Pop-Location}
Write-Host 'Local Nginx: http://127.0.0.1:8180 ; backend must allow this WebSocket Origin.'
