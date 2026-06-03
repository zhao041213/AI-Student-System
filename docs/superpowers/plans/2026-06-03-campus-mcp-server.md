# Campus MCP Server Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a local STDIO MCP server that exposes the AI Student System as five read-only agent tools: student search, student profile, student scores, employee search, and employee statistics.

**Architecture:** Add the official MCP Java SDK core dependency and run the MCP server through a Spring `mcp` profile. A profile-scoped `CommandLineRunner` starts the MCP STDIO server while `application-mcp.properties` disables the web server and keeps normal logs away from stdout. Tool business logic lives in a service class that reuses existing MyBatis mappers and services.

**Tech Stack:** Java 17, Spring Boot 2.7.5, MyBatis, MCP Java SDK `1.1.2`, Jackson, JUnit 5, Mockito, Maven.

---

## Scope Notes

The approved design mentioned a dedicated `CampusMcpApplication` entrypoint. The implementation should use a profile-scoped `CommandLineRunner` instead because the existing Spring Boot executable jar can then run MCP mode directly:

```powershell
java -jar java-backend/target/ikun-manager-backend-1.0.0.jar --spring.profiles.active=mcp
```

This keeps packaging simple and avoids adding a second executable jar.

## File Structure

- Modify: `java-backend/pom.xml`
  - Add MCP SDK dependencies using the official Java SDK artifacts.

- Create: `java-backend/src/main/resources/application-mcp.properties`
  - Disable web server and Spring banner in MCP mode.
  - Redirect logs away from stdout.

- Create: `java-backend/src/main/java/com/ikunmanager/mcp/CampusMcpProperties.java`
  - Hold limits such as `defaultLimit=20` and `maxLimit=50`.

- Create: `java-backend/src/main/java/com/ikunmanager/mcp/CampusMcpToolService.java`
  - Implement all read-only tool logic.
  - Reuse `StudentMapper`, `ScoreMapper`, `EmployeeMapper`, and `EmployeeService`.

- Create: `java-backend/src/main/java/com/ikunmanager/mcp/CampusMcpServerRunner.java`
  - Register MCP tools and start STDIO server under `mcp` profile.

- Create: `java-backend/src/main/java/com/ikunmanager/mcp/dto/*.java`
  - Request DTOs and response DTOs for tool input/output.

- Create: `java-backend/src/test/java/com/ikunmanager/mcp/CampusMcpToolServiceTest.java`
  - Test validation, privacy filtering, student lookup, scores, employee stats.

- Create: `java-backend/src/test/java/com/ikunmanager/mcp/CampusMcpServerRunnerTest.java`
  - Test tool names and schema registration without starting STDIO.

- Create: `docs/mcp/ai-student-system-mcp.json`
  - Example MCP client config.

---

## Task 1: Add MCP Dependencies and MCP Profile

**Files:**
- Modify: `java-backend/pom.xml`
- Create: `java-backend/src/main/resources/application-mcp.properties`

- [ ] **Step 1: Add MCP SDK dependencies**

Modify `java-backend/pom.xml` inside `<dependencies>`:

```xml
<dependency>
    <groupId>io.modelcontextprotocol.sdk</groupId>
    <artifactId>mcp-core</artifactId>
    <version>1.1.2</version>
</dependency>
<dependency>
    <groupId>io.modelcontextprotocol.sdk</groupId>
    <artifactId>mcp-json-jackson2</artifactId>
    <version>1.1.2</version>
</dependency>
```

- [ ] **Step 2: Add MCP profile configuration**

Create `java-backend/src/main/resources/application-mcp.properties`:

```properties
spring.main.web-application-type=none
spring.main.banner-mode=off
spring.main.lazy-initialization=false

campus.mcp.default-limit=20
campus.mcp.max-limit=50

logging.pattern.console=
logging.file.name=logs/mcp-server.log
logging.level.root=INFO
```

The empty console pattern prevents normal Spring logs from being written to stdout in MCP mode.

- [ ] **Step 3: Run dependency resolution**

Run:

```powershell
mvn "-Dmaven.repo.local=D:\AI-Student-System\.m2" test -DskipTests
```

Expected: Maven resolves MCP SDK dependencies and compiles without dependency conflicts.

- [ ] **Step 4: Commit setup**

Run:

```powershell
git add java-backend/pom.xml java-backend/src/main/resources/application-mcp.properties
git commit -m "Add MCP profile and SDK dependencies"
```

---

## Task 2: Create DTOs and Tool Service Tests

**Files:**
- Create: `java-backend/src/main/java/com/ikunmanager/mcp/dto/SearchStudentsRequest.java`
- Create: `java-backend/src/main/java/com/ikunmanager/mcp/dto/GetStudentProfileRequest.java`
- Create: `java-backend/src/main/java/com/ikunmanager/mcp/dto/GetStudentScoresRequest.java`
- Create: `java-backend/src/main/java/com/ikunmanager/mcp/dto/SearchEmployeesRequest.java`
- Create: `java-backend/src/main/java/com/ikunmanager/mcp/dto/ToolResult.java`
- Create: `java-backend/src/test/java/com/ikunmanager/mcp/CampusMcpToolServiceTest.java`

- [ ] **Step 1: Add request DTOs**

Create request DTOs with plain Java bean getters/setters so Jackson can deserialize MCP tool arguments.

`SearchStudentsRequest.java`:

```java
package com.ikunmanager.mcp.dto;

public class SearchStudentsRequest {
    private String name;
    private String studentId;
    private Integer limit;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}
```

`GetStudentProfileRequest.java`:

```java
package com.ikunmanager.mcp.dto;

public class GetStudentProfileRequest {
    private Long id;
    private String studentId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
}
```

`GetStudentScoresRequest.java`:

```java
package com.ikunmanager.mcp.dto;

public class GetStudentScoresRequest {
    private String studentId;
    private Long examId;
    private String subject;
    private Integer limit;

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}
```

`SearchEmployeesRequest.java`:

```java
package com.ikunmanager.mcp.dto;

public class SearchEmployeesRequest {
    private String name;
    private String empId;
    private String deptName;
    private String status;
    private Integer limit;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmpId() {
        return empId;
    }

    public void setEmpId(String empId) {
        this.empId = empId;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}
```

`ToolResult.java`:

```java
package com.ikunmanager.mcp.dto;

public class ToolResult<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> ToolResult<T> ok(T data) {
        ToolResult<T> result = new ToolResult<>();
        result.success = true;
        result.message = "OK";
        result.data = data;
        return result;
    }

    public static <T> ToolResult<T> error(String message) {
        ToolResult<T> result = new ToolResult<>();
        result.success = false;
        result.message = message;
        result.data = null;
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
```

- [ ] **Step 2: Write failing service tests**

Create `CampusMcpToolServiceTest.java`:

```java
package com.ikunmanager.mcp;

import com.ikunmanager.dto.EmployeeStatsDTO;
import com.ikunmanager.entity.Employee;
import com.ikunmanager.mapper.EmployeeMapper;
import com.ikunmanager.mapper.ScoreMapper;
import com.ikunmanager.mapper.StudentMapper;
import com.ikunmanager.mcp.dto.GetStudentProfileRequest;
import com.ikunmanager.mcp.dto.GetStudentScoresRequest;
import com.ikunmanager.mcp.dto.SearchEmployeesRequest;
import com.ikunmanager.mcp.dto.SearchStudentsRequest;
import com.ikunmanager.mcp.dto.ToolResult;
import com.ikunmanager.model.Score;
import com.ikunmanager.model.Student;
import com.ikunmanager.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampusMcpToolServiceTest {

    @Mock
    private StudentMapper studentMapper;

    @Mock
    private ScoreMapper scoreMapper;

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private CampusMcpToolService toolService;

    @Test
    void searchStudentsOmitsPhoneAndAppliesLimit() {
        Student student = sampleStudent();
        when(studentMapper.findAll("张", null)).thenReturn(Collections.singletonList(student));

        SearchStudentsRequest request = new SearchStudentsRequest();
        request.setName("张");
        request.setLimit(100);

        ToolResult<Map<String, Object>> result = toolService.searchStudents(request);

        assertThat(result.isSuccess()).isTrue();
        List<Map<String, Object>> students = (List<Map<String, Object>>) result.getData().get("students");
        assertThat(students).hasSize(1);
        assertThat(students.get(0)).containsEntry("studentId", "S2023001");
        assertThat(students.get(0)).doesNotContainKey("phone");
        assertThat(result.getData()).containsEntry("limit", 50);
    }

    @Test
    void getStudentProfileFindsByPublicStudentId() {
        Student student = sampleStudent();
        when(studentMapper.findAll(null, "S2023001")).thenReturn(Collections.singletonList(student));

        GetStudentProfileRequest request = new GetStudentProfileRequest();
        request.setStudentId("S2023001");

        ToolResult<Map<String, Object>> result = toolService.getStudentProfile(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).containsEntry("studentId", "S2023001");
        assertThat(result.getData()).containsEntry("phone", "15876627199");
    }

    @Test
    void getStudentScoresResolvesPublicStudentIdToInternalId() {
        Student student = sampleStudent();
        Score score = new Score();
        score.setId(9L);
        score.setStudentId(1L);
        score.setExamId(2L);
        score.setSubject("Math");
        score.setScore(new BigDecimal("95.5"));
        when(studentMapper.findAll(null, "S2023001")).thenReturn(Collections.singletonList(student));
        when(scoreMapper.findByPage(1L, null, null)).thenReturn(Collections.singletonList(score));

        GetStudentScoresRequest request = new GetStudentScoresRequest();
        request.setStudentId("S2023001");

        ToolResult<Map<String, Object>> result = toolService.getStudentScores(request);

        assertThat(result.isSuccess()).isTrue();
        List<Map<String, Object>> scores = (List<Map<String, Object>>) result.getData().get("scores");
        assertThat(scores.get(0)).containsEntry("subject", "Math");
        assertThat(scores.get(0)).containsEntry("score", new BigDecimal("95.5"));
    }

    @Test
    void searchEmployeesOmitsPhoneForBroadResults() {
        Employee employee = new Employee();
        employee.setId(3L);
        employee.setEmpId("EMP001");
        employee.setName("Employee A");
        employee.setPhone("13900000000");
        employee.setEmail("employee@example.com");
        employee.setDeptName("Tech");
        employee.setStatus("active");
        when(employeeMapper.findAll("Employee", null, null, null)).thenReturn(Collections.singletonList(employee));

        SearchEmployeesRequest request = new SearchEmployeesRequest();
        request.setName("Employee");

        ToolResult<Map<String, Object>> result = toolService.searchEmployees(request);

        assertThat(result.isSuccess()).isTrue();
        List<Map<String, Object>> employees = (List<Map<String, Object>>) result.getData().get("employees");
        assertThat(employees.get(0)).containsEntry("empId", "EMP001");
        assertThat(employees.get(0)).doesNotContainKey("phone");
    }

    @Test
    void getEmployeeStatsReturnsServiceData() {
        EmployeeStatsDTO stats = new EmployeeStatsDTO();
        when(employeeService.getEmployeeStatistics()).thenReturn(stats);

        ToolResult<EmployeeStatsDTO> result = toolService.getEmployeeStats();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isSameAs(stats);
    }

    private Student sampleStudent() {
        Student student = new Student();
        student.setId(1L);
        student.setStudentId("S2023001");
        student.setName("张伟");
        student.setGender("男");
        student.setClassId(1L);
        student.setClassName("一班");
        student.setPhone("15876627199");
        student.setEmail("student@example.com");
        return student;
    }
}
```

- [ ] **Step 3: Run tests and confirm red**

Run:

```powershell
mvn "-Dmaven.repo.local=D:\AI-Student-System\.m2" "-Dtest=CampusMcpToolServiceTest" test
```

Expected: compilation fails because `CampusMcpToolService` does not exist yet.

---

## Task 3: Implement MCP Tool Service

**Files:**
- Create: `java-backend/src/main/java/com/ikunmanager/mcp/CampusMcpProperties.java`
- Create: `java-backend/src/main/java/com/ikunmanager/mcp/CampusMcpToolService.java`
- Modify: `java-backend/src/test/java/com/ikunmanager/mcp/CampusMcpToolServiceTest.java`

- [ ] **Step 1: Add MCP properties**

Create `CampusMcpProperties.java`:

```java
package com.ikunmanager.mcp;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "campus.mcp")
public class CampusMcpProperties {
    private int defaultLimit = 20;
    private int maxLimit = 50;

    public int getDefaultLimit() {
        return defaultLimit;
    }

    public void setDefaultLimit(int defaultLimit) {
        this.defaultLimit = defaultLimit;
    }

    public int getMaxLimit() {
        return maxLimit;
    }

    public void setMaxLimit(int maxLimit) {
        this.maxLimit = maxLimit;
    }
}
```

- [ ] **Step 2: Implement tool service**

Create `CampusMcpToolService.java`:

```java
package com.ikunmanager.mcp;

import com.ikunmanager.dto.EmployeeStatsDTO;
import com.ikunmanager.entity.Employee;
import com.ikunmanager.mapper.EmployeeMapper;
import com.ikunmanager.mapper.ScoreMapper;
import com.ikunmanager.mapper.StudentMapper;
import com.ikunmanager.mcp.dto.GetStudentProfileRequest;
import com.ikunmanager.mcp.dto.GetStudentScoresRequest;
import com.ikunmanager.mcp.dto.SearchEmployeesRequest;
import com.ikunmanager.mcp.dto.SearchStudentsRequest;
import com.ikunmanager.mcp.dto.ToolResult;
import com.ikunmanager.model.Score;
import com.ikunmanager.model.Student;
import com.ikunmanager.service.EmployeeService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CampusMcpToolService {

    private final StudentMapper studentMapper;
    private final ScoreMapper scoreMapper;
    private final EmployeeMapper employeeMapper;
    private final EmployeeService employeeService;
    private final CampusMcpProperties properties;

    public CampusMcpToolService(StudentMapper studentMapper,
                                ScoreMapper scoreMapper,
                                EmployeeMapper employeeMapper,
                                EmployeeService employeeService,
                                CampusMcpProperties properties) {
        this.studentMapper = studentMapper;
        this.scoreMapper = scoreMapper;
        this.employeeMapper = employeeMapper;
        this.employeeService = employeeService;
        this.properties = properties;
    }

    public ToolResult<Map<String, Object>> searchStudents(SearchStudentsRequest request) {
        SearchStudentsRequest safeRequest = request == null ? new SearchStudentsRequest() : request;
        int limit = normalizeLimit(safeRequest.getLimit());
        List<Student> students = limit(studentMapper.findAll(blankToNull(safeRequest.getName()), blankToNull(safeRequest.getStudentId())), limit);

        Map<String, Object> data = new HashMap<>();
        data.put("students", students.stream().map(this::studentSearchRow).collect(Collectors.toList()));
        data.put("count", students.size());
        data.put("limit", limit);
        return ToolResult.ok(data);
    }

    public ToolResult<Map<String, Object>> getStudentProfile(GetStudentProfileRequest request) {
        if (request == null || (request.getId() == null && blankToNull(request.getStudentId()) == null)) {
            return ToolResult.error("Either id or studentId is required.");
        }
        Student student = request.getId() != null
                ? studentMapper.findById(request.getId())
                : findStudentByPublicId(request.getStudentId());
        if (student == null) {
            return ToolResult.error("Student not found.");
        }
        return ToolResult.ok(studentProfile(student));
    }

    public ToolResult<Map<String, Object>> getStudentScores(GetStudentScoresRequest request) {
        if (request == null || blankToNull(request.getStudentId()) == null) {
            return ToolResult.error("studentId is required.");
        }
        Student student = findStudentByPublicId(request.getStudentId());
        if (student == null) {
            return ToolResult.error("Student not found.");
        }
        int limit = normalizeLimit(request.getLimit());
        List<Score> scores = limit(scoreMapper.findByPage(student.getId(), request.getExamId(), blankToNull(request.getSubject())), limit);

        Map<String, Object> data = new HashMap<>();
        data.put("student", studentSearchRow(student));
        data.put("scores", scores.stream().map(this::scoreRow).collect(Collectors.toList()));
        data.put("count", scores.size());
        data.put("limit", limit);
        return ToolResult.ok(data);
    }

    public ToolResult<Map<String, Object>> searchEmployees(SearchEmployeesRequest request) {
        SearchEmployeesRequest safeRequest = request == null ? new SearchEmployeesRequest() : request;
        int limit = normalizeLimit(safeRequest.getLimit());
        List<Employee> employees = limit(employeeMapper.findAll(
                blankToNull(safeRequest.getName()),
                blankToNull(safeRequest.getEmpId()),
                blankToNull(safeRequest.getDeptName()),
                blankToNull(safeRequest.getStatus())
        ), limit);

        Map<String, Object> data = new HashMap<>();
        data.put("employees", employees.stream().map(this::employeeSearchRow).collect(Collectors.toList()));
        data.put("count", employees.size());
        data.put("limit", limit);
        return ToolResult.ok(data);
    }

    public ToolResult<EmployeeStatsDTO> getEmployeeStats() {
        return ToolResult.ok(employeeService.getEmployeeStatistics());
    }

    private Student findStudentByPublicId(String studentId) {
        List<Student> students = studentMapper.findAll(null, blankToNull(studentId));
        return students == null || students.isEmpty() ? null : students.get(0);
    }

    private Map<String, Object> studentSearchRow(Student student) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", student.getId());
        row.put("studentId", student.getStudentId());
        row.put("name", student.getName());
        row.put("gender", student.getGender());
        row.put("classId", student.getClassId());
        row.put("className", student.getClassName());
        row.put("email", student.getEmail());
        return row;
    }

    private Map<String, Object> studentProfile(Student student) {
        Map<String, Object> row = studentSearchRow(student);
        row.put("phone", student.getPhone());
        row.put("joinDate", student.getJoinDate());
        return row;
    }

    private Map<String, Object> scoreRow(Score score) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", score.getId());
        row.put("studentInternalId", score.getStudentId());
        row.put("examId", score.getExamId());
        row.put("subject", score.getSubject());
        row.put("score", score.getScore());
        row.put("createTime", score.getCreateTime());
        return row;
    }

    private Map<String, Object> employeeSearchRow(Employee employee) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", employee.getId());
        row.put("empId", employee.getEmpId());
        row.put("name", employee.getName());
        row.put("gender", employee.getGender());
        row.put("position", employee.getPosition());
        row.put("deptId", employee.getDeptId());
        row.put("deptName", employee.getDeptName());
        row.put("status", employee.getStatus());
        row.put("email", employee.getEmail());
        return row;
    }

    private <T> List<T> limit(List<T> values, int limit) {
        List<T> safeValues = values == null ? Collections.emptyList() : values;
        return safeValues.stream().limit(limit).collect(Collectors.toList());
    }

    private int normalizeLimit(Integer requestedLimit) {
        if (requestedLimit == null || requestedLimit <= 0) {
            return properties.getDefaultLimit();
        }
        return Math.min(requestedLimit, properties.getMaxLimit());
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
```

- [ ] **Step 3: Fix test construction**

Because `CampusMcpToolService` uses constructor injection, replace `@InjectMocks` in `CampusMcpToolServiceTest` with explicit setup:

```java
private CampusMcpToolService toolService;

@BeforeEach
void setUp() {
    CampusMcpProperties properties = new CampusMcpProperties();
    properties.setDefaultLimit(20);
    properties.setMaxLimit(50);
    toolService = new CampusMcpToolService(studentMapper, scoreMapper, employeeMapper, employeeService, properties);
}
```

Remove the `@InjectMocks` import and field.

- [ ] **Step 4: Run service tests**

Run:

```powershell
mvn "-Dmaven.repo.local=D:\AI-Student-System\.m2" "-Dtest=CampusMcpToolServiceTest" test
```

Expected: all `CampusMcpToolServiceTest` tests pass.

- [ ] **Step 5: Commit tool service**

Run:

```powershell
git add java-backend/src/main/java/com/ikunmanager/mcp java-backend/src/test/java/com/ikunmanager/mcp/CampusMcpToolServiceTest.java
git commit -m "Add campus MCP tool service"
```

---

## Task 4: Add MCP Server Runner and Tool Registry

**Files:**
- Create: `java-backend/src/main/java/com/ikunmanager/mcp/CampusMcpServerRunner.java`
- Create: `java-backend/src/test/java/com/ikunmanager/mcp/CampusMcpServerRunnerTest.java`

- [ ] **Step 1: Write tool registry test**

Create `CampusMcpServerRunnerTest.java`:

```java
package com.ikunmanager.mcp;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CampusMcpServerRunnerTest {

    @Test
    void toolSpecificationsExposeExpectedToolNames() {
        Set<String> names = CampusMcpServerRunner.toolSpecifications().stream()
                .map(CampusMcpServerRunner.ToolSpecification::getName)
                .collect(Collectors.toSet());

        assertThat(names).containsExactlyInAnyOrder(
                "search_students",
                "get_student_profile",
                "get_student_scores",
                "search_employees",
                "get_employee_stats"
        );
    }

    @Test
    void broadSearchToolsDescribePrivacyBehavior() {
        assertThat(CampusMcpServerRunner.toolSpecifications().stream()
                .filter(tool -> tool.getName().equals("search_students"))
                .findFirst()
                .orElseThrow()
                .getDescription())
                .contains("omits phone");

        assertThat(CampusMcpServerRunner.toolSpecifications().stream()
                .filter(tool -> tool.getName().equals("search_employees"))
                .findFirst()
                .orElseThrow()
                .getDescription())
                .contains("omits phone");
    }
}
```

- [ ] **Step 2: Run runner tests and confirm red**

Run:

```powershell
mvn "-Dmaven.repo.local=D:\AI-Student-System\.m2" "-Dtest=CampusMcpServerRunnerTest" test
```

Expected: compilation fails because `CampusMcpServerRunner` does not exist yet.

- [ ] **Step 3: Implement runner skeleton and registry**

Create `CampusMcpServerRunner.java` with a static registry first. Use MCP SDK classes after confirming the exact imported class names from the resolved dependency.

```java
package com.ikunmanager.mcp;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@Profile("mcp")
public class CampusMcpServerRunner implements CommandLineRunner {

    private final CampusMcpToolService toolService;

    public CampusMcpServerRunner(CampusMcpToolService toolService) {
        this.toolService = toolService;
    }

    @Override
    public void run(String... args) throws Exception {
        startServer();
    }

    void startServer() throws Exception {
        // MCP SDK server wiring is added in the next step after dependency APIs are verified.
    }

    static List<ToolSpecification> toolSpecifications() {
        return Arrays.asList(
                new ToolSpecification("search_students", "Search students by name or studentId; broad results omit phone numbers."),
                new ToolSpecification("get_student_profile", "Get one student profile by id or studentId."),
                new ToolSpecification("get_student_scores", "Get scores for a student by public studentId, optionally filtered by examId and subject."),
                new ToolSpecification("search_employees", "Search employees by name, empId, department, or status; broad results omit phone numbers."),
                new ToolSpecification("get_employee_stats", "Get aggregate employee statistics.")
        );
    }

    static class ToolSpecification {
        private final String name;
        private final String description;

        ToolSpecification(String name, String description) {
            this.name = name;
            this.description = description;
        }

        String getName() {
            return name;
        }

        String getDescription() {
            return description;
        }
    }
}
```

- [ ] **Step 4: Run registry tests**

Run:

```powershell
mvn "-Dmaven.repo.local=D:\AI-Student-System\.m2" "-Dtest=CampusMcpServerRunnerTest" test
```

Expected: tests pass.

- [ ] **Step 5: Wire MCP SDK server**

Use the resolved MCP SDK API to replace `startServer()` with STDIO server startup and register each tool. If the SDK API differs from the snippet below, inspect the local `.m2` sources/javadocs and adapt class names while preserving the same tool names, descriptions, and JSON schemas.

The final runner must:

- create STDIO transport
- create a synchronous MCP server
- register all five tools
- serialize `ToolResult` with Jackson
- block while the MCP server is running

The implementation should keep tool JSON schema strings near the registry. Each schema must be an object schema with explicit properties. Example schema for `search_students`:

```json
{
  "type": "object",
  "properties": {
    "name": { "type": "string" },
    "studentId": { "type": "string" },
    "limit": { "type": "integer", "minimum": 1, "maximum": 50 }
  }
}
```

- [ ] **Step 6: Add a smokeable manual command**

Run:

```powershell
mvn "-Dmaven.repo.local=D:\AI-Student-System\.m2" package
java -jar target/ikun-manager-backend-1.0.0.jar --spring.profiles.active=mcp
```

Expected: the process starts without Spring web server logs on stdout. Stop it manually after confirming startup.

- [ ] **Step 7: Commit runner**

Run:

```powershell
git add java-backend/src/main/java/com/ikunmanager/mcp/CampusMcpServerRunner.java java-backend/src/test/java/com/ikunmanager/mcp/CampusMcpServerRunnerTest.java
git commit -m "Add campus MCP server runner"
```

---

## Task 5: Add MCP Client Configuration and Documentation

**Files:**
- Create: `docs/mcp/ai-student-system-mcp.json`
- Create: `docs/mcp/README.md`

- [ ] **Step 1: Add example MCP client config**

Create `docs/mcp/ai-student-system-mcp.json`:

```json
{
  "mcpServers": {
    "ai-student-system": {
      "command": "java",
      "args": [
        "-jar",
        "D:/AI-Student-System/java-backend/target/ikun-manager-backend-1.0.0.jar",
        "--spring.profiles.active=mcp"
      ]
    }
  }
}
```

- [ ] **Step 2: Add README**

Create `docs/mcp/README.md`:

```markdown
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
```

- [ ] **Step 3: Commit docs**

Run:

```powershell
git add docs/mcp
git commit -m "Document campus MCP server usage"
```

---

## Task 6: Verification

**Files:**
- No new files.

- [ ] **Step 1: Run targeted MCP tests**

Run:

```powershell
mvn "-Dmaven.repo.local=D:\AI-Student-System\.m2" "-Dtest=CampusMcpToolServiceTest,CampusMcpServerRunnerTest" test
```

Expected: all MCP tests pass.

- [ ] **Step 2: Run full backend tests**

Run:

```powershell
mvn "-Dmaven.repo.local=D:\AI-Student-System\.m2" test
```

Expected: all backend tests pass.

- [ ] **Step 3: Build jar**

Run:

```powershell
mvn "-Dmaven.repo.local=D:\AI-Student-System\.m2" package -DskipTests
```

Expected: `target/ikun-manager-backend-1.0.0.jar` is built.

- [ ] **Step 4: Start MCP mode briefly**

Run:

```powershell
java -jar target/ikun-manager-backend-1.0.0.jar --spring.profiles.active=mcp
```

Expected:

- No Tomcat startup message.
- No Spring banner on stdout.
- MCP process stays running for client connections.

- [ ] **Step 5: Confirm REST mode still works**

Start the normal backend:

```powershell
mvn "-Dmaven.repo.local=D:\AI-Student-System\.m2" spring-boot:run
```

Login smoke test:

```powershell
$body = @{ username = 'S2023001'; password = '123456' } | ConvertTo-Json
Invoke-RestMethod -Uri 'http://localhost:8081/api/auth/login' -Method Post -ContentType 'application/json' -Body $body
```

Expected: response contains `accessToken` and `tokenType`.

- [ ] **Step 6: Final status**

Run:

```powershell
git status --short
```

Expected: no uncommitted source changes except ignored build output.

---

## Self-Review

- Spec coverage: covers STDIO MCP server, five read-only tools, privacy rules, no raw SQL, logging isolation, example client config, tests, and REST preservation.
- Placeholder scan: no task uses placeholder markers or unspecified "add appropriate" work.
- Type consistency: request DTO names, service method names, and test method names match across tasks.
- Scope check: write tools, trace UI, guardrails, and HTTP/SSE transport are intentionally excluded from this first implementation.
