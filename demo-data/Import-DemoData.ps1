param([string]$JavaHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot -Parent
if (!(Test-Path (Join-Path $project '.env'))) { throw 'Configure the project .env and start CampusConnect once with the bootstrap profile first.' }
$java = if ($JavaHome) { Join-Path $JavaHome 'bin/java.exe' } else { 'java' }
$javac = if ($JavaHome) { Join-Path $JavaHome 'bin/javac.exe' } else { 'javac' }
$jar = Join-Path $project 'dist/campusconnect-1.0.0.jar'
if (!(Test-Path $jar)) { $jar = Join-Path $project 'target/campusconnect-1.0.0.jar' }
if (!(Test-Path $jar)) { throw 'Build the project with mvn clean package first.' }
$runtime = Join-Path $project 'target/demo-import'
New-Item -ItemType Directory -Force "$runtime/lib", "$runtime/classes" | Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [System.IO.Compression.ZipFile]::OpenRead($jar)
try {
  foreach ($entry in $archive.Entries) {
    if ($entry.FullName -like 'BOOT-INF/lib/*.jar') {
      [System.IO.Compression.ZipFileExtensions]::ExtractToFile($entry, (Join-Path "$runtime/lib" $entry.Name), $true)
    }
  }
} finally { $archive.Dispose() }
& $javac -encoding UTF-8 -cp "$runtime/lib/*" -d "$runtime/classes" "$PSScriptRoot/PopulateDemo.java"
if ($LASTEXITCODE -ne 0) { throw 'Dataset importer compilation failed. Use JDK 21.' }
& $java -cp "$runtime/classes;$runtime/lib/*" PopulateDemo $project
if ($LASTEXITCODE -ne 0) { throw 'Dataset import failed; see error above.' }
