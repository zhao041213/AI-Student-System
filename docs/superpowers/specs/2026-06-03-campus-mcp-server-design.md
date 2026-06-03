# Campus MCP Server Design

## Goal

Build a local STDIO MCP server for the AI Student System so agent clients such as Cursor, Claude Desktop, or Claude Code can discover and call campus-management tools against the existing Spring Boot backend.

The first version is intentionally read-only. It demonstrates agent tool design, structured business access, and safe data exposure without adding risky write operations such as deleting students, changing scores, or batch-modifying employees.

## Resume Value

This feature turns the project from a conventional Vue + Spring Boot CRUD system into an agent-ready platform:

- Traditional frontend users keep using REST APIs.
- Agent clients use MCP tools to query students, scores, classes, and employees.
- The MCP server exposes narrow business tools rather than raw SQL.
- The implementation can be discussed in interviews around tool schemas, transport, logging isolation, permission boundaries, and testability.

## Chosen Approach

Use the official MCP Java SDK core server with STDIO transport, launched through a dedicated Spring profile named `mcp`.

Do not use Spring AI MCP Boot Starter for the first version. The backend currently runs Spring Boot 2.7.5, while current Spring AI starter lines are more naturally aligned with newer Spring Boot versions. Using the MCP Java SDK core avoids a broad framework upgrade and keeps the change focused.

## Non-Goals

- No frontend chat UI in this version.
- No write tools in this version.
- No direct SQL execution tool.
- No LLM provider integration inside the backend.
- No remote HTTP/SSE MCP transport in this version.
- No OAuth or multi-tenant authentication for the MCP transport in this version.

## Architecture

The MCP server runs as a local process started by the agent client. It loads the Spring application context, reuses existing MyBatis mappers and service classes, and registers tool handlers with the MCP Java SDK.

Data flow:

1. User asks an agent client a natural-language question.
2. The agent client discovers available MCP tools.
3. The agent decides which tool to call and sends JSON arguments over STDIO.
4. The MCP server validates arguments.
5. The MCP tool calls existing Spring services or mappers.
6. The MCP server returns structured JSON results.
7. The agent summarizes or continues with additional tool calls.

STDIO matters because MCP protocol messages use stdout. Normal application logs must not be written to stdout in MCP mode. The MCP profile will disable the Spring banner and route logs to a file or stderr-only logger configuration.

## Project Structure

Planned files:

- `java-backend/src/main/java/com/ikunmanager/mcp/CampusMcpApplication.java`
  - Dedicated MCP entrypoint.
  - Starts Spring with `mcp` profile.
  - Does not start the web server.

- `java-backend/src/main/java/com/ikunmanager/mcp/CampusMcpServer.java`
  - Owns MCP server creation.
  - Registers tools and tool handlers.
  - Keeps transport/protocol setup separate from business logic.

- `java-backend/src/main/java/com/ikunmanager/mcp/CampusMcpTools.java`
  - Contains tool handler methods.
  - Calls existing `StudentMapper`, `ScoreMapper`, `ClassMapper`, `EmployeeMapper`, and `EmployeeService`.
  - Converts model objects into safe response DTOs/maps.

- `java-backend/src/main/java/com/ikunmanager/mcp/dto/*.java`
  - Request/response DTOs for tool arguments and structured outputs.

- `java-backend/src/main/resources/application-mcp.properties`
  - MCP profile configuration.
  - Disables web server and banner.
  - Configures logging so stdout stays clean.

- `java-backend/src/test/java/com/ikunmanager/mcp/*.java`
  - Unit tests for tool handlers and schema behavior.

- `docs/mcp/ai-student-system-mcp.json`
  - Example MCP client config for local use.

## Tool Set

### `search_students`

Search students by optional `name` and/or `studentId`.

Arguments:

```json
{
  "name": "张",
  "studentId": "S2023001",
  "limit": 20
}
```

Result:

```json
{
  "students": [
    {
      "id": 1,
      "studentId": "S2023001",
      "name": "张伟",
      "gender": "男",
      "classId": 1,
      "className": "一班",
      "email": "student@example.com"
    }
  ],
  "count": 1
}
```

Privacy rule: phone numbers are omitted from default search results.

### `get_student_profile`

Fetch one student by internal numeric `id` or public `studentId`.

Arguments:

```json
{
  "studentId": "S2023001"
}
```

Result includes student profile fields and class information. It can include phone/email because this is a targeted profile lookup, not a broad search.

### `get_student_scores`

Fetch scores for a student, optionally filtered by `examId` and `subject`.

Arguments:

```json
{
  "studentId": "S2023001",
  "examId": 1,
  "subject": "数学"
}
```

Implementation detail: current `ScoreMapper` expects the internal student primary key, while users often know the public student number. The tool will first resolve public `studentId` through `StudentMapper.findAll`, then call score mapper methods with the internal id.

### `search_employees`

Search employees by optional `name`, `empId`, `deptName`, and `status`.

Arguments:

```json
{
  "name": "李",
  "deptName": "技术部",
  "status": "在职",
  "limit": 20
}
```

Result includes employee id, employee number, name, department, position, status, and email. Phone is omitted from broad search results.

### `get_employee_stats`

Return aggregate employee statistics from existing `EmployeeService`.

Arguments:

```json
{}
```

Result contains totals, active/inactive counts, salary aggregates, and distribution data already supported by the employee module.

## Safety Rules

- No tool accepts arbitrary SQL.
- No tool performs inserts, updates, deletes, import, or export in v1.
- Broad search tools omit phone numbers by default.
- Tool arguments are validated before calling mappers.
- `limit` defaults to 20 and caps at 50.
- Errors return structured messages suitable for agent reasoning, not raw stack traces.

## Logging

MCP mode must keep stdout reserved for protocol messages.

Required MCP profile behavior:

- Disable Spring Boot banner.
- Disable or redirect console logging away from stdout.
- Write application logs to `java-backend/logs/mcp-server.log` or stderr.
- Do not use `System.out.println` anywhere in MCP code.

## Packaging and Running

Build:

```powershell
mvn "-Dmaven.repo.local=D:\AI-Student-System\.m2" package
```

Run manually:

```powershell
java -cp "target/ikun-manager-backend-1.0.0.jar" com.ikunmanager.mcp.CampusMcpApplication
```

Example MCP client config:

```json
{
  "mcpServers": {
    "ai-student-system": {
      "command": "java",
      "args": [
        "-cp",
        "D:/AI-Student-System/java-backend/target/ikun-manager-backend-1.0.0.jar",
        "com.ikunmanager.mcp.CampusMcpApplication"
      ],
      "env": {
        "SPRING_PROFILES_ACTIVE": "mcp"
      }
    }
  }
}
```

If the packaged jar cannot launch a non-default main class cleanly, add a small script under `docs/mcp/` or `scripts/` that starts the MCP entrypoint with the correct classpath.

## Testing

Automated tests:

- Tool handler tests with mocked mappers/services.
- Argument validation tests.
- Privacy tests confirming broad search responses omit phone numbers.
- Error handling tests for missing student, invalid limit, and empty filters.

Manual smoke test:

1. Build backend jar.
2. Add `docs/mcp/ai-student-system-mcp.json` config to an MCP-compatible client.
3. Ask: "查一下 S2023001 的学生信息。"
4. Confirm the client calls `get_student_profile`.
5. Ask: "查询 S2023001 的成绩。"
6. Confirm the client calls `get_student_scores`.
7. Ask: "统计员工情况。"
8. Confirm the client calls `get_employee_stats`.

## Acceptance Criteria

- MCP server starts in STDIO mode without writing normal logs to stdout.
- Agent client can discover all five tools.
- All five tools return structured JSON results.
- Search tools enforce default and maximum limits.
- Broad search tools do not expose phone numbers.
- Existing REST backend behavior remains unchanged.
- Maven tests pass.
- A sample MCP client configuration is committed.

## Future Extensions

- Add write tools behind human approval.
- Add tool-call tracing and a frontend trace viewer.
- Add guardrails for role-based data access.
- Add HTTP/SSE MCP transport for remote deployments.
- Add eval cases for common campus-management tasks.
