# ATC MCP Server

An AI-ready Air Traffic Control [Model Context Protocol](https://modelcontextprotocol.io)
server. Connected MCP clients can submit flight plans, generate a deterministic
airport schedule across runways and gates, inspect operational status, react to
disruptions, and analyse the longest scheduled dependency chain.

The server is implemented in Java 21 with Spring Boot 3 and the
`spring-ai-starter-mcp-server-webmvc` starter. Transport is HTTP/SSE.

---

## Table of contents

1. [Prerequisites](#prerequisites)
2. [Install dependencies and build](#install-dependencies-and-build)
3. [Environment variables](#environment-variables)
4. [Run the server](#run-the-server)
5. [Connect from an MCP-compatible client](#connect-from-an-mcp-compatible-client)
6. [Tool reference](#tool-reference)
7. [Resource reference](#resource-reference)
8. [Validation scenarios](#validation-scenarios)
9. [Troubleshooting](#troubleshooting)

---

## Prerequisites

- **JDK 21** (or newer). Verify with `java -version`.
- **Apache Maven 3.9+**. Verify with `mvn -version`.
- A working network port (default `8080`). For testing, an MCP-compatible client
  such as the official [MCP Inspector](https://modelcontextprotocol.io/docs/tools/inspector),
  Cursor, or Claude Desktop.

## Install dependencies and build

```bash
# from task-4/
mvn clean package
```

The command:

- Downloads dependencies from Maven Central (Spring Boot 3.4.2, Spring AI 1.0.0,
  the official `io.modelcontextprotocol` Java SDK).
- Compiles sources, runs the **17 included tests** (unit + scenario tests
  mirroring `validation_scenarios.md`), and produces a runnable Spring Boot fat
  jar at `target/atc-mcp-server-1.0.0.jar`.

To skip tests during a quick iteration:

```bash
mvn -DskipTests=true package
```

## Environment variables

All airport limits are loaded from environment variables. The startup validator
fails fast with a friendly diagnostic if anything is missing or out of range.

| Variable | Type | Default | Accepted values | Description |
|----------|------|---------|------------------|-------------|
| `ATC_RUNWAYS` | comma list of `id:lengthMeters` | `R1:3500,R2:2500,R3:4000` | non-empty list, unique ids, positive integer lengths | Defines the runways and their physical capability. A flight whose `minRunwayLengthMeters` exceeds every runway is marked `UNSCHEDULED(NO_SUITABLE_RUNWAY)`. |
| `ATC_GATE_COUNT` | integer | `6` | positive | Total number of parking/turnaround gates. |
| `ATC_GROUND_CREW_COUNT` | integer | `8` | positive | Reported in airport status; not yet a hard scheduling constraint. |
| `ATC_SEPARATION_TAKEOFF_SECONDS` | integer | `120` | non-negative | Minimum gap on the same runway between two consecutive *takeoffs*. |
| `ATC_SEPARATION_LANDING_SECONDS` | integer | `180` | non-negative | Minimum gap on the same runway between two consecutive *landings*. |
| `ATC_SEPARATION_MIXED_SECONDS` | integer | `240` | non-negative | Minimum gap on the same runway between a landing and a takeoff (or vice versa). |
| `ATC_GATE_TURNAROUND_SECONDS` | integer | `1800` | non-negative | After an operation finishes, the gate stays busy for this long before another aircraft can use it. |
| `ATC_DEPENDENCY_BUFFER_SECONDS` | integer | `900` | non-negative | Mandatory buffer inserted between a flight's dependency end-time and the dependent flight's start-time. |
| `ATC_SCHEDULING_HORIZON_SECONDS` | integer | `43200` (12 h) | positive, ≥ longest operation duration | Scheduler refuses to schedule operations beyond this many seconds after the epoch. |
| `ATC_OPERATION_DURATION_ARRIVAL_SECONDS` | integer | `600` | positive | Default duration of an arrival operation. |
| `ATC_OPERATION_DURATION_DEPARTURE_SECONDS` | integer | `480` | positive | Default duration of a departure operation. |
| `ATC_EPOCH` | ISO-8601 instant | `2026-01-01T00:00:00Z` | any valid `Instant` | Wall-clock origin used to render absolute timestamps. The scheduler itself is deterministic and does **not** read the system clock. |
| `ATC_SERVER_PORT` | integer | `8080` | 1–65535 | HTTP port for the SSE transport and Actuator endpoints. |
| `ATC_SSE_MESSAGE_ENDPOINT` | string | `/mcp/messages` | starts with `/` | URL where MCP clients POST their JSON-RPC messages. The SSE handshake URL is always `/sse`. |

A ready-to-copy `.env.example` is included alongside this file.

## Run the server

### Bash / Linux / macOS

```bash
export ATC_RUNWAYS="R1:3500,R2:2500,R3:4000"
export ATC_GATE_COUNT=6
java -jar target/atc-mcp-server-1.0.0.jar
```

### PowerShell / Windows

```powershell
$env:ATC_RUNWAYS = "R1:3500,R2:2500,R3:4000"
$env:ATC_GATE_COUNT = 6
java -jar target\atc-mcp-server-1.0.0.jar
```

### Maven (development)

```bash
mvn spring-boot:run
```

On a successful boot you should see:

```
... McpServerAutoConfiguration : Registered tools: 5
... McpServerAutoConfiguration : Registered resources: 3
... AtcMcpApplication       : Started AtcMcpApplication in 2.x seconds
```

The server then exposes:

- SSE handshake: `http://localhost:8080/sse`
- JSON-RPC POST endpoint: `http://localhost:8080/mcp/messages` (configurable via
  `ATC_SSE_MESSAGE_ENDPOINT`)
- Actuator health endpoint: `http://localhost:8080/actuator/health`

## Connect from an MCP-compatible client

### MCP Inspector

The MCP Inspector is the easiest way to verify the server. From any directory:

```bash
npx @modelcontextprotocol/inspector
```

In the Inspector UI choose **SSE** as the transport and enter the URL
`http://localhost:8080/sse`. After connecting you will see the five tools and
three resources listed in this README.

### Cursor

Edit `.cursor/mcp.json` (or use the Cursor settings UI) and add:

```json
{
  "mcpServers": {
    "atc": {
      "url": "http://localhost:8080/sse"
    }
  }
}
```

Then restart Cursor and pick the **atc** server when invoking tools.

### Claude Desktop

Edit `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "atc": {
      "url": "http://localhost:8080/sse"
    }
  }
}
```

(If you use a stdio bridge such as `mcp-remote`, point it at the same SSE URL.)

## Tool reference

All tools return an envelope of the shape:

```json
{
  "ok": true,
  "code": "<machine_readable_code>",
  "message": null,
  "data": { /* tool-specific payload */ }
}
```

On a recoverable error (`duplicate_flight`, `invalid_arguments`, `flight_not_found`)
`ok` is `false`, `data` is `null`, and `message` carries a human-readable
explanation.

| Tool | Description | Required parameters | Optional parameters |
|------|-------------|---------------------|---------------------|
| `submit_flight` | Add a new flight to the queue in `PENDING` state. The flight is considered the next time the schedule is regenerated. | `flightNumber` (string), `operationType` (`ARRIVAL` \| `DEPARTURE`), `priority` (`HIGH` \| `MEDIUM` \| `LOW`) | `dependencies` (list of flight numbers), `minRunwayLengthMeters` (int) |
| `cancel_flight` | Mark a flight as `CANCELLED`. Flights depending on it are flagged for re-evaluation on the next schedule generation. | `flightNumber` | – |
| `generate_schedule` | Recompute the airport schedule from the current queue and configuration. Replaces the existing schedule with a deterministic, freshly computed one. | – | – |
| `get_airport_status` | Returns flight counts by status and operation type, runway and gate capacity/usage, resource constraint indicators, unscheduled flights with reasons, and the schedule completion time when available. | – | – |
| `analyze_bottleneck` | Identifies the longest active scheduled dependency chain — the ordered flight numbers, the total elapsed duration accounting for operation durations and dependency buffers, and start/end timestamps. | – | – |

### Example: submit a flight

```json
{
  "name": "submit_flight",
  "arguments": {
    "flightNumber": "BA101",
    "operationType": "ARRIVAL",
    "priority": "HIGH"
  }
}
```

### Example: connecting flight

```json
{
  "name": "submit_flight",
  "arguments": {
    "flightNumber": "OUT1",
    "operationType": "DEPARTURE",
    "priority": "HIGH",
    "dependencies": ["INB1"]
  }
}
```

### Example: get_airport_status response

```json
{
  "ok": true,
  "code": "ok",
  "data": {
    "flightCountsByStatus": { "SCHEDULED": 4, "PENDING": 0, "UNSCHEDULED": 0, "CANCELLED": 0 },
    "flightCountsByOperationType": { "ARRIVAL": 2, "DEPARTURE": 2 },
    "totalFlights": 4,
    "runwayCount": 3,
    "gateCount": 6,
    "groundCrewCount": 8,
    "scheduleCompletionSec": 1080,
    "scheduleCompletionAt": "2026-01-01T00:18:00Z",
    "resourceConstrained": false,
    "unscheduledFlights": [],
    "runwayCapacity": [
      { "runwayId": "R1", "lengthMeters": 3500, "scheduledOperations": 2, "busySeconds": 1080 },
      { "runwayId": "R2", "lengthMeters": 2500, "scheduledOperations": 1, "busySeconds": 480 },
      { "runwayId": "R3", "lengthMeters": 4000, "scheduledOperations": 1, "busySeconds": 600 }
    ]
  }
}
```

## Resource reference

All resources return JSON (`mimeType: application/json`).

| URI | Description |
|-----|-------------|
| `atc://flights/queue` | Every known flight, grouped into `scheduled`, `unscheduled`, `pending`, and `cancelled`. Each flight includes its `unscheduledReason` and `unscheduledDetails` when applicable. |
| `atc://runways` | Per-runway list: `runwayId`, `lengthMeters`, count and total busy seconds, and the chronological list of operations currently using the runway. |
| `atc://timeline` | Chronological list of every scheduled operation with relative seconds (`startSec`, `endSec`) and absolute timestamps derived from `ATC_EPOCH` (`startAt`, `endAt`). |

## Validation scenarios

The tests under `src/test/java/com/atc/mcp/scenarios/` mirror the validation
scenarios in `validation_scenarios.md`:

| Scenario | Test class |
|----------|------------|
| Morning Rush | `MorningRushScenarioTest` |
| Heavy Hauler | `HeavyHaulerScenarioTest` |
| Connecting Flight | `ConnectingFlightScenarioTest` |
| Cancellation propagation | `CancellationScenarioTest` |
| Determinism (extra) | `DeterminismScenarioTest` |

Run them with `mvn test`.

## Troubleshooting

- **`Invalid airport configuration: ...` at startup.** A `FailureAnalyzer` prints
  exactly which environment variable is wrong and how to fix it. Check the
  variable mentioned and the [Environment variables](#environment-variables)
  table.
- **Port already in use.** Set `ATC_SERVER_PORT` to a free port.
- **Client says "tools list is empty".** Confirm the server log shows
  `Registered tools: 5` and that the client used the `/sse` URL on the same
  port. The MCP Inspector is the simplest sanity check.
- **`NO_SLOT_WITHIN_HORIZON` for everything.** The horizon is shorter than the
  combined required durations. Increase `ATC_SCHEDULING_HORIZON_SECONDS`.
