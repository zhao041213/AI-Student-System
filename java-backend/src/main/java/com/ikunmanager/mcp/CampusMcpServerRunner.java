package com.ikunmanager.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikunmanager.mcp.dto.GetStudentProfileRequest;
import com.ikunmanager.mcp.dto.GetStudentScoresRequest;
import com.ikunmanager.mcp.dto.SearchEmployeesRequest;
import com.ikunmanager.mcp.dto.SearchStudentsRequest;
import com.ikunmanager.mcp.dto.ToolResult;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

@Component
@Profile("mcp")
public class CampusMcpServerRunner implements CommandLineRunner {

    private final CampusMcpToolService toolService;
    private final ObjectMapper objectMapper;

    public CampusMcpServerRunner(CampusMcpToolService toolService, ObjectMapper objectMapper) {
        this.toolService = toolService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        startServer();
    }

    void startServer() throws InterruptedException {
        StdioServerTransportProvider transportProvider =
                new StdioServerTransportProvider(McpJsonDefaults.getMapper());

        McpSyncServer server = McpServer.sync(transportProvider)
                .serverInfo("ai-student-system", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
                .tools(toolSpecifications().stream()
                        .map(this::toSyncToolSpecification)
                        .collect(Collectors.toList()))
                .build();

        CountDownLatch keepAlive = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.closeGracefully();
            keepAlive.countDown();
        }));
        keepAlive.await();
    }

    private McpServerFeatures.SyncToolSpecification toSyncToolSpecification(ToolSpecification specification) {
        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name(specification.getName())
                .description(specification.getDescription())
                .inputSchema(McpJsonDefaults.getMapper(), specification.getInputSchema())
                .build();

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) -> callTool(request.name(), request.arguments()))
                .build();
    }

    private McpSchema.CallToolResult callTool(String name, Map<String, Object> arguments) {
        try {
            Map<String, Object> safeArguments = arguments == null ? Map.of() : arguments;
            switch (name) {
                case "search_students":
                    return toolResult(toolService.searchStudents(
                            objectMapper.convertValue(safeArguments, SearchStudentsRequest.class)));
                case "get_student_profile":
                    return toolResult(toolService.getStudentProfile(
                            objectMapper.convertValue(safeArguments, GetStudentProfileRequest.class)));
                case "get_student_scores":
                    return toolResult(toolService.getStudentScores(
                            objectMapper.convertValue(safeArguments, GetStudentScoresRequest.class)));
                case "search_employees":
                    return toolResult(toolService.searchEmployees(
                            objectMapper.convertValue(safeArguments, SearchEmployeesRequest.class)));
                case "get_employee_stats":
                    return toolResult(toolService.getEmployeeStats());
                default:
                    return toolResult(ToolResult.error("Unknown tool: " + name));
            }
        } catch (Exception e) {
            return toolResult(ToolResult.error("Tool execution failed: " + e.getMessage()));
        }
    }

    private McpSchema.CallToolResult toolResult(ToolResult<?> result) {
        Object structuredContent = objectMapper.convertValue(result, Map.class);
        String textContent;
        try {
            textContent = objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            textContent = result.getMessage();
        }

        return McpSchema.CallToolResult.builder()
                .addTextContent(textContent)
                .structuredContent(structuredContent)
                .isError(!result.isSuccess())
                .build();
    }

    static List<ToolSpecification> toolSpecifications() {
        return Arrays.asList(
                new ToolSpecification(
                        "search_students",
                        "Search students by name or studentId; this broad search omits phone numbers.",
                        """
                        {
                          "type": "object",
                          "properties": {
                            "name": { "type": "string" },
                            "studentId": { "type": "string" },
                            "limit": { "type": "integer", "minimum": 1, "maximum": 50 }
                          }
                        }
                        """
                ),
                new ToolSpecification(
                        "get_student_profile",
                        "Get one student profile by id or studentId.",
                        """
                        {
                          "type": "object",
                          "properties": {
                            "id": { "type": "integer" },
                            "studentId": { "type": "string" }
                          }
                        }
                        """
                ),
                new ToolSpecification(
                        "get_student_scores",
                        "Get scores for a student by public studentId, optionally filtered by examId and subject.",
                        """
                        {
                          "type": "object",
                          "properties": {
                            "studentId": { "type": "string" },
                            "examId": { "type": "integer" },
                            "subject": { "type": "string" },
                            "limit": { "type": "integer", "minimum": 1, "maximum": 50 }
                          },
                          "required": ["studentId"]
                        }
                        """
                ),
                new ToolSpecification(
                        "search_employees",
                        "Search employees by name, empId, department, or status; this broad search omits phone numbers.",
                        """
                        {
                          "type": "object",
                          "properties": {
                            "name": { "type": "string" },
                            "empId": { "type": "string" },
                            "deptName": { "type": "string" },
                            "status": { "type": "string" },
                            "limit": { "type": "integer", "minimum": 1, "maximum": 50 }
                          }
                        }
                        """
                ),
                new ToolSpecification(
                        "get_employee_stats",
                        "Get aggregate employee statistics.",
                        """
                        {
                          "type": "object",
                          "properties": {}
                        }
                        """
                )
        );
    }

    static class ToolSpecification {
        private final String name;
        private final String description;
        private final String inputSchema;

        ToolSpecification(String name, String description, String inputSchema) {
            this.name = name;
            this.description = description;
            this.inputSchema = inputSchema;
        }

        String getName() {
            return name;
        }

        String getDescription() {
            return description;
        }

        String getInputSchema() {
            return inputSchema;
        }
    }
}
