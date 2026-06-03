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
        List<Student> students = limit(
                studentMapper.findAll(blankToNull(safeRequest.getName()), blankToNull(safeRequest.getStudentId())),
                limit
        );

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
        List<Score> scores = limit(
                scoreMapper.findByPage(student.getId(), request.getExamId(), blankToNull(request.getSubject())),
                limit
        );

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
