# Run Maven with a valid JAVA_HOME (JDK 21) on Windows when JAVA_HOME is missing or wrong.
# Usage: .\scripts\run.ps1 spring-boot:run
#        .\scripts\run.ps1 -DskipTests verify

param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $MavenArgs
)

function Test-JavaHome([string] $JdkRoot) {
    if (-not $JdkRoot) { return $false }
    $java = Join-Path $JdkRoot "bin\java.exe"
    return (Test-Path $java)
}

function Get-Jdk21HomeFromJavaOnPath {
    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    if (-not $javaCmd -or -not $javaCmd.Source) { return $null }

    $javaExe = $javaCmd.Source
    if (-not (Test-Path -LiteralPath $javaExe)) { return $null }

    $binDir = Split-Path -Parent $javaExe
    $leaf = Split-Path -Leaf $javaExe
    if ($leaf -ne 'java.exe') { return $null }

    $candidate = $null
    if ((Split-Path -Leaf $binDir) -eq 'bin') {
        $candidate = Split-Path -Parent $binDir
    } else {
        $candidate = $binDir
    }

    if (-not (Test-JavaHome $candidate)) { return $null }
    $javac = Join-Path $candidate "bin\javac.exe"
    if (-not (Test-Path -LiteralPath $javac)) { return $null }

    $javaBin = Join-Path $candidate "bin\java.exe"
    try {
        $ver = & $javaBin -version 2>&1 | Out-String
        if ($ver -notmatch 'version "21') { return $null }
    } catch {
        return $null
    }

    return $candidate
}

function Find-Jdk21Home {
    $roots = @(
        "$env:ProgramFiles\Java",
        "$env:ProgramFiles\Eclipse Adoptium",
        "$env:ProgramFiles\Microsoft",
        "$env:LocalAppData\Programs\Eclipse Adoptium",
        "$env:LocalAppData\Programs\Microsoft"
    )
    foreach ($root in $roots) {
        if (-not (Test-Path $root)) { continue }
        foreach ($dir in (Get-ChildItem $root -Directory -ErrorAction SilentlyContinue)) {
            $javaExe = Join-Path $dir.FullName "bin\java.exe"
            if (-not (Test-Path $javaExe)) { continue }
            try {
                $ver = & $javaExe -version 2>&1 | Out-String
                if ($ver -match 'version "21') {
                    return $dir.FullName
                }
            } catch {
                continue
            }
        }
    }
    return $null
}

if (-not (Test-JavaHome $env:JAVA_HOME)) {
    $found = Get-Jdk21HomeFromJavaOnPath
    if (-not $found) {
        $found = Find-Jdk21Home
    }
    if ($found) {
        $env:JAVA_HOME = $found
        $bin = Join-Path $found "bin"
        $env:PATH = "$bin;$env:PATH"
        Write-Host "JAVA_HOME was unset or invalid; using: $env:JAVA_HOME" -ForegroundColor Yellow
    } else {
        Write-Host @"
JAVA_HOME is not set correctly and JDK 21 was not found under common install paths.

Fix options:
  1) Install JDK 21, e.g.: winget install Microsoft.OpenJDK.21
  2) Set JAVA_HOME to the JDK root (folder that contains bin\java.exe), then reopen the terminal:
       [Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Path\To\jdk-21', 'User')
  3) For this session only:
       `$env:JAVA_HOME = 'C:\Path\To\jdk-21'
       `$env:PATH = "`$env:JAVA_HOME\bin;`$env:PATH"

Then run: mvn $($MavenArgs -join ' ')
"@ -ForegroundColor Red
        exit 1
    }
}

$mvn = Get-Command mvn -ErrorAction SilentlyContinue
if (-not $mvn) {
    Write-Error "mvn not found in PATH. Install Maven or add it to PATH."
    exit 1
}

& mvn @MavenArgs
exit $LASTEXITCODE
