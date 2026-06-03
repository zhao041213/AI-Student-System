# AI Student System MCP Server

This project exposes selected campus-management capabilities as read-only MCP tools for local agent clients.

## Build

```powershell
cd D:\AI-Student-System\java-backend
mvn "-Dmaven.repo.local=D:\AI-Student-System\.m2" package
```

## Tools

- `search_students`: search students by name or public student id. Broad results omit phone numbers.
- `get_student_profile`: get one student profile by id or public student id.
- `get_student_scores`: get scores for a student by public student id.
- `search_employees`: search employees by name, employee id, department, or status. Broad results omit phone numbers.
- `get_employee_stats`: get aggregate employee statistics.

## Client Config

Use `docs/mcp/ai-student-system-mcp.json` as an example configuration for MCP-compatible clients.

## Safety

The first version is read-only. It does not expose raw SQL, deletes, score edits, imports, exports, or batch updates.
