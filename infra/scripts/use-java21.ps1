# Dot-source after loading .env. Prefer a valid JAVA_HOME, then local/PATH fallbacks.
$ErrorActionPreference = 'Stop'
$citygoJavaCandidates = @($env:JAVA_HOME, 'D:\jdks\temurin-21')
$citygoJavac = Get-Command javac -ErrorAction SilentlyContinue
if ($citygoJavac) { $citygoJavaCandidates += Split-Path -Parent (Split-Path -Parent $citygoJavac.Source) }
$citygoJavaFound = $false
foreach ($citygoCandidate in ($citygoJavaCandidates | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Unique)) {
    $citygoJavaExe = Join-Path $citygoCandidate 'bin/java.exe'
    if (-not (Test-Path -LiteralPath $citygoJavaExe)) { continue }
    try { $citygoJavaVersion = (& $citygoJavaExe --version 2>$null | Out-String) }
    catch { continue }
    if ($LASTEXITCODE -eq 0 -and $citygoJavaVersion -match '(?m)^(?:openjdk|java)\s+21(?:[.\s]|$)') {
        $env:JAVA_HOME = $citygoCandidate
        $citygoJavaFound = $true
        break
    }
}
if (-not $citygoJavaFound) { throw 'Java 21 is required. Set JAVA_HOME to your JDK 21 installation.' }
