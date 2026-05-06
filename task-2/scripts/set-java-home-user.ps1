# Sets persistent User environment variable JAVA_HOME from the JDK used by `java` on PATH.
# Requires JDK 21 (directory layout: ...\jdk\...\bin\java.exe + bin\javac.exe).
# Run: powershell -ExecutionPolicy Bypass -File .\scripts\set-java-home-user.ps1

$javaCmd = Get-Command java -ErrorAction SilentlyContinue
if (-not $javaCmd -or -not $javaCmd.Source) {
    Write-Error "java not found in PATH."
    exit 1
}

$javaExe = $javaCmd.Source
if ((Split-Path -Leaf $javaExe) -ne "java.exe") {
    Write-Error "Unexpected java command target: $javaExe"
    exit 1
}

$binDir = Split-Path -Parent $javaExe

if ((Split-Path -Leaf $binDir) -eq "bin") {
    $candidate = Split-Path -Parent $binDir
} else {
    $candidate = $binDir
}

$javac = Join-Path $candidate "bin\javac.exe"
if (-not (Test-Path -LiteralPath $javac)) {
    Write-Error "javac.exe not found under $candidate - use a JDK, not a JRE-only install."
    exit 1
}

$javaBin = Join-Path $candidate "bin\java.exe"
$psi = [System.Diagnostics.ProcessStartInfo]::new($javaBin, "-version")
$psi.RedirectStandardError = $true
$psi.RedirectStandardOutput = $true
$psi.UseShellExecute = $false
$psi.CreateNoWindow = $true
$p = [System.Diagnostics.Process]::Start($psi)
$ver = ($p.StandardError.ReadToEnd() + $p.StandardOutput.ReadToEnd()).Trim()
[void] $p.WaitForExit(10000)
$p.Dispose()

if ($ver.IndexOf('version "21', [System.StringComparison]::Ordinal) -lt 0) {
    Write-Error "This JDK is not Java 21 (java -version output: $ver)"
    exit 1
}

[Environment]::SetEnvironmentVariable("JAVA_HOME", $candidate, "User")
Write-Host "JAVA_HOME (User scope) set to: $candidate" -ForegroundColor Green
Write-Host "Open a new terminal, or for this session run:" -ForegroundColor Yellow
Write-Host ('  $env:JAVA_HOME = "' + $candidate + '"') -ForegroundColor Yellow
