# Build the official fixed source locally; no third-party prebuilt MinIO image is needed.
param([switch]$SkipBuild)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$privateDir=Join-Path $projectRoot 'data/private'
New-Item -ItemType Directory -Path $privateDir -Force | Out-Null
$stateFile=Join-Path $privateDir 'minio-state.json'
if(Test-Path -LiteralPath $stateFile){$state=Get-Content -LiteralPath $stateFile -Raw|ConvertFrom-Json}else{
 $state=[pscustomobject]@{user='citygo';password=([Guid]::NewGuid().ToString('N')+[Guid]::NewGuid().ToString('N'));endpoint='http://127.0.0.1:9000'}
 $state|ConvertTo-Json|Set-Content -LiteralPath $stateFile -Encoding utf8
}
$env:CITYGO_MINIO_ROOT_USER=$state.user;$env:CITYGO_MINIO_ROOT_PASSWORD=$state.password
Push-Location $projectRoot
try{
 if(-not $SkipBuild){docker compose --env-file .env -f infra/docker-compose.yml --profile minio build minio;if($LASTEXITCODE -ne 0){throw 'MinIO source build failed'}}
 docker compose --env-file .env -f infra/docker-compose.yml --profile minio up -d minio;if($LASTEXITCODE -ne 0){throw 'MinIO startup failed'}
 $ready=$false;for($attempt=0;$attempt -lt 30;$attempt++){try{$r=Invoke-WebRequest "$($state.endpoint)/minio/health/live";if($r.StatusCode -eq 200){$ready=$true;break}}catch{};Start-Sleep -Seconds 2}
 if(-not $ready){throw 'MinIO health check timed out'}
}finally{Pop-Location}
Write-Host 'MinIO ready: http://127.0.0.1:9000 ; console: http://127.0.0.1:9001'
Write-Host 'Local credentials are stored in ignored data/private/minio-state.json; do not commit or publish them.'
