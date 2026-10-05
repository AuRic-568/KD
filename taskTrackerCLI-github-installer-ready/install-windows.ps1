$ErrorActionPreference = 'Stop'
$repo = Split-Path -Parent $MyInvocation.MyCommand.Path
$jar = Join-Path $repo 'TaskTracker.jar'
if (!(Test-Path $jar)) { throw 'TaskTracker.jar is missing. Download the full release/repository.' }

$java = Get-Command java -ErrorAction SilentlyContinue
if (!$java) { throw 'Java 17+ is required. Install a JDK/JRE 17 or newer, then run this installer again.' }

$majorText = (& java -version 2>&1 | Select-Object -First 1) -replace '.*version "([0-9]+).*','$1'
if ([int]$majorText -lt 17) { throw 'Java 17 or newer is required.' }

$installDir = Join-Path $env:LOCALAPPDATA 'TaskTracker'
New-Item -ItemType Directory -Force -Path $installDir | Out-Null
Copy-Item $jar (Join-Path $installDir 'TaskTracker.jar') -Force

$launcher = Join-Path $installDir 'TaskTracker.cmd'
@"
@echo off
cd /d "$installDir"
start "" javaw -jar TaskTracker.jar
"@ | Set-Content -Encoding ASCII $launcher

$ws = New-Object -ComObject WScript.Shell
$startMenu = Join-Path $env:APPDATA 'Microsoft\Windows\Start Menu\Programs\TaskTracker.lnk'
$shortcut = $ws.CreateShortcut($startMenu)
$shortcut.TargetPath = $launcher
$shortcut.WorkingDirectory = $installDir
$shortcut.Description = 'TaskTracker'
$shortcut.Save()

Write-Host "TaskTracker installed. Open it from the Start menu." -ForegroundColor Green
