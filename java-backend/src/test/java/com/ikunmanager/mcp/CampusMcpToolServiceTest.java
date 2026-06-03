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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    private CampusMcpToolService toolService;

    @BeforeEach
    void setUp() {
        CampusMcpProperties properties = new CampusMcpProperties();
        properties.setDefaultLimit(20);
        properties.setMaxLimit(50);
        toolService = new CampusMcpToolService(studentMapper, scoreMapper, employeeMapper, employeeService, properties);
    }

    @Test
    void searchStudentsOmitsPhoneAndAppliesLimit() {
        Student student = sampleStudent();
        when(studentMapper.findAll("张", null)).thenReturn(Collections.singletonList(student));

        SearchStudentsRequest request = new SearchStudentsRequest();
        request.setName("张");
        request.setLimit(100);

        ToolResult<Map<String, Object>> result = toolService.searchStudents(request);

        assertThat(result.isSuccess()).isTrue();
        List<?> students = (List<?>) result.getData().get("students");
        Map<?, ?> firstStudent = (Map<?, ?>) students.get(0);
        assertThat(students).hasSize(1);
        assertThat(firstStudent.get("studentId")).isEqualTo("S2023001");
        assertThat(firstStudent.containsKey("phone")).isFalse();
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
        List<?> scores = (List<?>) result.getData().get("scores");
        Map<?, ?> firstScore = (Map<?, ?>) scores.get(0);
        assertThat(firstScore.get("subject")).isEqualTo("Math");
        assertThat(firstScore.get("score")).isEqualTo(new BigDecimal("95.5"));
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
        List<?> employees = (List<?>) result.getData().get("employees");
        Map<?, ?> firstEmployee = (Map<?, ?>) employees.get(0);
        assertThat(firstEmployee.get("empId")).isEqualTo("EMP001");
        assertThat(firstEmployee.containsKey("phone")).isFalse();
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
