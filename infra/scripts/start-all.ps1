# Start local services in hidden windows. Logs remain in ignored data/logs.
param([int]$BackendPort=8080,[int]$FrontendPort=5173)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$logRoot=Join-Path $projectRoot 'data/logs'
New-Item -ItemType Directory -Path $logRoot -Force | Out-Null
if(Get-NetTCPConnection -LocalPort $BackendPort,$FrontendPort -State Listen -ErrorAction SilentlyContinue){throw 'Requested port already has a listener; choose free ports.'}
$env:SERVER_PORT="$BackendPort";$env:SERVER_ADDRESS='127.0.0.1';$env:CITYGO_API_PROXY="http://127.0.0.1:$BackendPort"
$env:CITYGO_WEBSOCKET_ALLOWED_ORIGINS="http://127.0.0.1:$FrontendPort,http://localhost:$FrontendPort"
$backend=Start-Process powershell -WindowStyle Hidden -ArgumentList '-NoProfile','-File',(Join-Path $PSScriptRoot 'run-backend.ps1') -WorkingDirectory $projectRoot -RedirectStandardOutput (Join-Path $logRoot 'backend.log') -RedirectStandardError (Join-Path $logRoot 'backend.err.log') -PassThru
$healthy=$false
for($attempt=0;$attempt -lt 90;$attempt++){try{$r=Invoke-RestMethod "http://127.0.0.1:$BackendPort/actuator/health";if($r.status -eq 'UP'){$healthy=$true;break}}catch{};if($backend.HasExited){throw 'Backend exited; see data/logs.'};Start-Sleep -Seconds 2}
if(-not $healthy){throw 'Backend readiness timed out; see data/logs.'}
Start-Process powershell -WindowStyle Hidden -ArgumentList '-NoProfile','-File',(Join-Path $PSScriptRoot 'run-frontend.ps1'),'-Port',$FrontendPort -WorkingDirectory $projectRoot -RedirectStandardOutput (Join-Path $logRoot 'frontend.log') -RedirectStandardError (Join-Path $logRoot 'frontend.err.log') | Out-Null
Write-Host "Backend ready: http://127.0.0.1:$BackendPort ; frontend starting: http://127.0.0.1:$FrontendPort"
