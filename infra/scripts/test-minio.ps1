# Opt-in real MinIO acceptance. Requires the local MinIO service and isolated citygo_test database.
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$envFile=Join-Path $projectRoot '.env'
if(Test-Path -LiteralPath $envFile){Get-Content -LiteralPath $envFile|Where-Object {$_ -match '^\s*[A-Za-z_][A-Za-z0-9_]*='}|ForEach-Object{$key,$value=$_ -split '=',2;if(-not [Environment]::GetEnvironmentVariable($key.Trim(),'Process')){[Environment]::SetEnvironmentVariable($key.Trim(),$value.Trim(),'Process')}}}
$stateFile=Join-Path $projectRoot 'data/private/minio-state.json'
if(-not (Test-Path -LiteralPath $stateFile)){throw 'Run start-minio.ps1 first; local MinIO credentials are missing.'}
$state=Get-Content -LiteralPath $stateFile -Raw|ConvertFrom-Json
$env:CITYGO_STORAGE_S3_ACCESS_KEY=$state.user;$env:CITYGO_STORAGE_S3_SECRET_KEY=$state.password
. (Join-Path $PSScriptRoot 'use-java21.ps1')
$health=Invoke-WebRequest 'http://127.0.0.1:9000/minio/health/live'
if($health.StatusCode -ne 200){throw 'Local MinIO is not healthy'}
Push-Location (Join-Path $projectRoot 'backend')
try{.\mvnw.cmd '-Dtest=S3FileUploadAcceptanceIT' test;$testExit=$LASTEXITCODE}finally{Pop-Location}
exit $testExit
