# Destructive test cleanup is allowed only in a newly created, isolated citygo_test database.
# Existing containers and data volumes are never reused or removed automatically.
param([ValidatePattern('^[a-z0-9][a-z0-9-]+$')][string]$ContainerName='citygo-mysql84-acceptance',[ValidateRange(1024,65535)][int]$Port=3307)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$volumeName="$ContainerName-data"
if(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue){throw 'Requested MySQL test port is already occupied.'}
$existing=docker ps -a --format '{{.Names}}'
if($existing -contains $ContainerName){throw 'Container already exists. Choose a new ContainerName; existing data will not be deleted.'}
$volumes=docker volume ls --format '{{.Name}}'
if($volumes -contains $volumeName){throw 'Volume already exists. Choose a new ContainerName; existing data will not be reused.'}
$privateDir=Join-Path $projectRoot 'data/private'
New-Item -ItemType Directory -Path $privateDir -Force | Out-Null
$state=[ordered]@{container=$ContainerName;volume=$volumeName;port=$Port;user='citygo_test';password=[Guid]::NewGuid().ToString('N');rootPassword=[Guid]::NewGuid().ToString('N')}
$statePath=Join-Path $privateDir "$ContainerName.json"
if(Test-Path -LiteralPath $statePath){throw 'State file exists; choose a new ContainerName.'}
$state|ConvertTo-Json|Set-Content -LiteralPath $statePath -Encoding utf8
docker volume create --label citygo.purpose=mysql84-acceptance $volumeName | Out-Null
if($LASTEXITCODE -ne 0){throw 'Volume creation failed'}
docker run -d --name $ContainerName --label citygo.purpose=mysql84-acceptance --publish "127.0.0.1:${Port}:3306" --mount "type=volume,src=$volumeName,dst=/var/lib/mysql" -e MYSQL_DATABASE=citygo_test -e MYSQL_USER=citygo_test -e "MYSQL_PASSWORD=$($state.password)" -e "MYSQL_ROOT_PASSWORD=$($state.rootPassword)" mysql:8.4@sha256:0f51e63e537a1f176423fa03ef559aabe6fa4372a53160a44bc7e75da7d15364 | Out-Null
if($LASTEXITCODE -ne 0){throw 'MySQL 8.4 startup failed; original databases were not touched.'}
$ready=$false
for($attempt=0;$attempt -lt 120;$attempt++){
 $answer=docker exec -e "MYSQL_PWD=$($state.password)" $ContainerName mysql '--host=127.0.0.1' '--user=citygo_test' '--database=citygo_test' --batch --skip-column-names -e 'SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()' 2>$null
 if($LASTEXITCODE -eq 0){if([int]$answer -ne 0){throw 'New acceptance database is not empty; refusing cleanup'};$ready=$true;break}
 Start-Sleep -Seconds 2
}
if(-not $ready){throw 'MySQL 8.4 initialization timed out; inspect container state.'}
Write-Host 'Fresh MySQL 8.4 database is empty; applying Flyway V1-V4 through the real test suite.'
$env:CITYGO_TEST_DB_URL="jdbc:mysql://127.0.0.1:$Port/citygo_test?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false"
$env:CITYGO_DB_USER='citygo_test';$env:CITYGO_DB_PASSWORD=$state.password
& (Join-Path $PSScriptRoot 'run-backend-tests.ps1')
exit $LASTEXITCODE
