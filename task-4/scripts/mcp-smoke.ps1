# Минимальный MCP-клиент для smoke-проверки сервера через SSE.
# 1. Открывает GET /sse и получает endpoint URL.
# 2. Делает initialize, initialized.
# 3. Вызывает tools/list, resources/list.
# 4. Вызывает submit_flight x4, generate_schedule.
# 5. Читает ресурс atc://timeline.

param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"

# В Windows PowerShell 5.1 System.Net.Http не подгружен по умолчанию.
Add-Type -AssemblyName System.Net.Http

# Открываем SSE-поток в фоне.
$sseClient = [System.Net.Http.HttpClient]::new()
$sseClient.Timeout = [TimeSpan]::FromMinutes(2)
$sseRequest = [System.Net.Http.HttpRequestMessage]::new(
    [System.Net.Http.HttpMethod]::Get, "$BaseUrl/sse")
$sseRequest.Headers.Accept.Add(
    [System.Net.Http.Headers.MediaTypeWithQualityHeaderValue]::new("text/event-stream"))
$sseResponse = $sseClient.SendAsync($sseRequest, [System.Net.Http.HttpCompletionOption]::ResponseHeadersRead).Result
$sseStream = $sseResponse.Content.ReadAsStreamAsync().Result
$sseReader = [System.IO.StreamReader]::new($sseStream)

Write-Host "== SSE connected: HTTP $($sseResponse.StatusCode) ==" -ForegroundColor Cyan

# Ждём endpoint-событие и достаём URL для POST.
$messageEndpoint = $null
$currentEvent = $null
while (-not $sseReader.EndOfStream) {
    $line = $sseReader.ReadLine()
    if ($line -eq $null) { break }
    if ($line.StartsWith("event:")) {
        $currentEvent = $line.Substring(6).Trim()
    } elseif ($line.StartsWith("data:")) {
        $data = $line.Substring(5).Trim()
        if ($currentEvent -eq "endpoint") {
            if ($data.StartsWith("http")) {
                $messageEndpoint = $data
            } else {
                $messageEndpoint = "$BaseUrl$data"
            }
            break
        }
    }
}
if (-not $messageEndpoint) { throw "Did not receive endpoint event from /sse" }
Write-Host "messageEndpoint: $messageEndpoint" -ForegroundColor Yellow

# Запускаем runspace, который собирает SSE-сообщения в очередь.
$incoming = [System.Collections.Concurrent.ConcurrentQueue[string]]::new()
$ps = [powershell]::Create()
$ps.AddScript({
    param($reader, $queue)
    $event = $null
    while ($true) {
        try { $line = $reader.ReadLine() } catch { break }
        if ($line -eq $null) { break }
        if ($line.StartsWith("event:")) {
            $event = $line.Substring(6).Trim()
        } elseif ($line.StartsWith("data:")) {
            $data = $line.Substring(5).Trim()
            if ($event -eq "message") {
                $queue.Enqueue($data) | Out-Null
            }
        }
    }
}).AddArgument($sseReader).AddArgument($incoming) | Out-Null
$async = $ps.BeginInvoke()

function Send-McpRequest {
    param([string]$method, $params, [int]$id, [bool]$expectResponse = $true)

    $payload = @{
        jsonrpc = "2.0"
        method  = $method
    }
    if ($id -ne -1) { $payload.id = $id }
    if ($params -ne $null) { $payload.params = $params }
    $body = $payload | ConvertTo-Json -Depth 10 -Compress

    Invoke-RestMethod -Uri $script:messageEndpoint -Method POST `
        -ContentType "application/json" -Body $body | Out-Null

    if (-not $expectResponse) { return $null }

    $deadline = (Get-Date).AddSeconds(5)
    while ((Get-Date) -lt $deadline) {
        $msg = $null
        if ($script:incoming.TryDequeue([ref]$msg)) {
            $parsed = $msg | ConvertFrom-Json
            if ($parsed.id -eq $id) { return $parsed }
        }
        Start-Sleep -Milliseconds 50
    }
    throw "Timeout waiting for response to id $id ($method)"
}

# 1) initialize
$init = Send-McpRequest -method "initialize" -id 1 -params @{
    protocolVersion = "2024-11-05"
    capabilities    = @{}
    clientInfo      = @{ name = "smoke.ps1"; version = "1.0" }
}
Write-Host "`n== initialize -> $($init.result.serverInfo.name) v$($init.result.serverInfo.version) ==" -ForegroundColor Cyan

Send-McpRequest -method "notifications/initialized" -id -1 -params @{} -expectResponse $false

# 2) tools/list
$tools = Send-McpRequest -method "tools/list" -id 2 -params @{}
Write-Host "`n== tools/list ==" -ForegroundColor Cyan
$tools.result.tools | ForEach-Object { "  - {0}: {1}" -f $_.name, ($_.description.Substring(0, [Math]::Min(80, $_.description.Length))) } | Write-Host

# 3) resources/list
$resources = Send-McpRequest -method "resources/list" -id 3 -params @{}
Write-Host "`n== resources/list ==" -ForegroundColor Cyan
$resources.result.resources | ForEach-Object { "  - {0} ({1})" -f $_.uri, $_.mimeType } | Write-Host

# 4) submit 4 flights covering Morning Rush
function Submit-Flight {
    param([int]$id, [string]$num, [string]$op, [string]$prio, [int[]]$rwLen, [string[]]$deps)
    $args = @{
        flightNumber  = $num
        operationType = $op
        priority      = $prio
    }
    if ($deps) { $args.dependencies = $deps }
    if ($rwLen) { $args.minRunwayLengthMeters = $rwLen[0] }
    Send-McpRequest -method "tools/call" -id $id -params @{
        name      = "submit_flight"
        arguments = $args
    }
}

$null = Submit-Flight -id 10 -num "BA101" -op "ARRIVAL"   -prio "HIGH"
$null = Submit-Flight -id 11 -num "LH202" -op "DEPARTURE" -prio "MEDIUM"
$null = Submit-Flight -id 12 -num "AF303" -op "ARRIVAL"   -prio "LOW"
$null = Submit-Flight -id 13 -num "KL404" -op "DEPARTURE" -prio "LOW"
# Heavy Hauler: длина больше любой ВПП => UNSCHEDULED
$null = Submit-Flight -id 14 -num "HEAVY1" -op "DEPARTURE" -prio "HIGH" -rwLen @(6000)
# Connecting flight: OUT1 зависит от INB1
$null = Submit-Flight -id 15 -num "INB1" -op "ARRIVAL"   -prio "MEDIUM"
$null = Submit-Flight -id 16 -num "OUT1" -op "DEPARTURE" -prio "HIGH" -deps @("INB1")
Write-Host "`n== submitted 7 flights (4 morning rush + 1 heavy hauler + 2 connecting) ==" -ForegroundColor Cyan

# 5) generate_schedule
$gen = Send-McpRequest -method "tools/call" -id 20 -params @{
    name      = "generate_schedule"
    arguments = @{}
}
$genData = ($gen.result.content[0].text | ConvertFrom-Json).data
Write-Host "`n== generate_schedule: scheduled=$($genData.scheduled.Count), unscheduled=$($genData.unscheduled.Count), cancelled=$($genData.cancelled.Count) ==" -ForegroundColor Cyan
$genData.scheduled | ForEach-Object {
    "  + {0,-7} {1,-9} {2,-9} runway={3} gate={4} start={5}s end={6}s" -f `
        $_.flightNumber, $_.operationType, $_.priority, `
        $_.assignedRunwayId, $_.assignedGateId, $_.scheduledStartSec, $_.scheduledEndSec
} | Write-Host
$genData.unscheduled | ForEach-Object {
    "  ! {0,-7} reason={1} ({2})" -f $_.flightNumber, $_.unscheduledReason, $_.unscheduledDetails
} | Write-Host

# 6) get_airport_status
$status = Send-McpRequest -method "tools/call" -id 30 -params @{
    name      = "get_airport_status"
    arguments = @{}
}
$statusData = ($status.result.content[0].text | ConvertFrom-Json).data
Write-Host "`n== get_airport_status ==" -ForegroundColor Cyan
"  flightCountsByStatus: $($statusData.flightCountsByStatus | ConvertTo-Json -Compress)" | Write-Host
"  scheduleCompletionAt: $($statusData.scheduleCompletionAt)" | Write-Host
"  resourceConstrained:  $($statusData.resourceConstrained)" | Write-Host

# 7) analyze_bottleneck
$bn = Send-McpRequest -method "tools/call" -id 40 -params @{
    name      = "analyze_bottleneck"
    arguments = @{}
}
$bnData = ($bn.result.content[0].text | ConvertFrom-Json).data
Write-Host "`n== analyze_bottleneck ==" -ForegroundColor Cyan
"  exists=$($bnData.exists), chain=$($bnData.flightNumbers -join ' -> '), totalDurationSeconds=$($bnData.totalDurationSeconds)" | Write-Host

# 8) read resource atc://timeline
$timeline = Send-McpRequest -method "resources/read" -id 50 -params @{
    uri = "atc://timeline"
}
$timelineJson = $timeline.result.contents[0].text | ConvertFrom-Json
Write-Host "`n== resource atc://timeline (first 5 entries) ==" -ForegroundColor Cyan
$timelineJson | Select-Object -First 5 | ForEach-Object {
    "  {0,-7} {1,-9} runway={2} gate={3} {4}" -f `
        $_.flightNumber, $_.operationType, $_.runwayId, $_.gateId, $_.startAt
} | Write-Host

Write-Host "`n== smoke OK ==" -ForegroundColor Green

# Cleanup
try { $sseReader.Dispose() } catch {}
try { $sseClient.Dispose() } catch {}
try { $ps.Stop() } catch {}
try { $ps.Dispose() } catch {}
