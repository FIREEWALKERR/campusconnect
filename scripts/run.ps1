param([switch]$UseJar)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$environmentFile = Join-Path $projectRoot '.env'
if (!(Test-Path -LiteralPath $environmentFile)) {
    throw 'Copy .env.example to .env and enter your database and first-administrator settings before running.'
}
foreach ($line in Get-Content -LiteralPath $environmentFile) {
    if ($line -match '^\s*(#|$)') { continue }
    $pair = $line -split '=', 2
    if ($pair.Count -ne 2 -or $pair[0].Trim() -notmatch '^[A-Z][A-Z0-9_]*$') {
        throw 'Invalid .env line. Use NAME=value, one variable per line.'
    }
    $value = $pair[1].Trim()
    if (($value.StartsWith('"') -and $value.EndsWith('"')) -or
        ($value.StartsWith("'") -and $value.EndsWith("'"))) {
        $value = $value.Substring(1, $value.Length - 2)
    }
    [Environment]::SetEnvironmentVariable($pair[0].Trim(), $value, 'Process')
}
foreach ($name in @('DB_URL', 'DB_USERNAME', 'DB_PASSWORD')) {
    if (![Environment]::GetEnvironmentVariable($name, 'Process')) { throw "Set $name in .env." }
}
Push-Location $projectRoot
try {
    if ($UseJar) {
        $jar = Join-Path $projectRoot 'dist/campusconnect-1.0.0.jar'
        if (!(Test-Path -LiteralPath $jar)) { $jar = Join-Path $projectRoot 'target/campusconnect-1.0.0.jar' }
        if (!(Test-Path -LiteralPath $jar)) { throw 'Build the JAR first with mvn clean package.' }
        & java -jar $jar
    } else {
        & mvn spring-boot:run
    }
    if ($LASTEXITCODE -ne 0) { throw "Application exited with code $LASTEXITCODE. See the preceding startup error." }
} finally { Pop-Location }
