package com.ikunmanager.controller;

import com.alibaba.excel.EasyExcel;
import com.ikunmanager.common.ApiResponse;
import com.ikunmanager.dto.EmployeeStatsDTO;
import com.ikunmanager.entity.Employee;
import com.ikunmanager.mapper.EmployeeMapper;
import com.ikunmanager.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/employee")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final EmployeeMapper employeeMapper;

    @Autowired
    public EmployeeController(EmployeeService employeeService, EmployeeMapper employeeMapper) {
        this.employeeService = employeeService;
        this.employeeMapper = employeeMapper;
    }

    @GetMapping("/list")
    public ApiResponse<List<Employee>> getEmployeeList(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String empId,
            @RequestParam(required = false) String deptName,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.ok(employeeMapper.findAll(name, empId, deptName, status));
    }

    @PostMapping("/add")
    public ApiResponse<Employee> addEmployee(@RequestBody Employee employee) {
        LocalDateTime now = LocalDateTime.now();
        if (employee.getCreateTime() == null) {
            employee.setCreateTime(now);
        }
        employee.setUpdateTime(now);

        employeeMapper.insert(employee);
        if (employee.getId() != null) {
            Employee savedEmployee = employeeMapper.findById(employee.getId());
            if (savedEmployee != null) {
                return ApiResponse.ok(savedEmployee);
            }
        }
        return ApiResponse.ok(employee);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportEmployees(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String empId,
            @RequestParam(required = false) String deptName,
            @RequestParam(required = false) String status
    ) {
        List<Employee> employees = employeeMapper.findAll(name, empId, deptName, status);
        byte[] workbook = buildEmployeeWorkbook(employees);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"employees.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(workbook);
    }

    @PostMapping("/import")
    public ApiResponse<Map<String, Object>> importEmployees(@RequestParam("file") MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "Import file is empty");
        }

        List<Employee> employees = parseEmployeeFile(file);
        if (employees.isEmpty()) {
            return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "Import file has no employee rows");
        }

        int imported = employeeMapper.batchInsert(employees);
        Map<String, Object> result = new HashMap<>();
        result.put("imported", imported);
        return ApiResponse.ok(result);
    }

    @DeleteMapping("/batch")
    public ApiResponse<Map<String, Object>> batchDeleteEmployees(@RequestBody Map<String, List<Long>> request) {
        List<Long> ids = request.get("ids");
        if (ids == null || ids.isEmpty()) {
            return ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "Employee ids are required");
        }

        int deleted = employeeMapper.batchDelete(ids);
        Map<String, Object> result = new HashMap<>();
        result.put("deleted", deleted);
        return ApiResponse.ok(result);
    }

    @GetMapping("/stats")
    public ApiResponse<EmployeeStatsDTO> getEmployeeStats() {
        try {
            EmployeeStatsDTO stats = employeeService.getEmployeeStatistics();
            return ApiResponse.ok(stats);
        } catch (Exception e) {
            return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Failed to get employee statistics: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ApiResponse<Employee> updateEmployee(@PathVariable Long id, @RequestBody Employee employee) {
        Employee existingEmployee = employeeMapper.findById(id);
        if (existingEmployee == null) {
            return ApiResponse.error(HttpStatus.NOT_FOUND.value(), "Employee not found");
        }

        employee.setId(id);
        if (employee.getCreateTime() == null) {
            employee.setCreateTime(existingEmployee.getCreateTime());
        }
        employee.setUpdateTime(LocalDateTime.now());

        employeeMapper.update(employee);
        Employee updatedEmployee = employeeMapper.findById(id);
        return ApiResponse.ok(updatedEmployee != null ? updatedEmployee : employee);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, Object>> deleteEmployee(@PathVariable Long id) {
        Employee existingEmployee = employeeMapper.findById(id);
        if (existingEmployee == null) {
            return ApiResponse.error(HttpStatus.NOT_FOUND.value(), "Employee not found");
        }

        int deleted = employeeMapper.delete(id);
        Map<String, Object> result = new HashMap<>();
        result.put("deleted", deleted);
        return ApiResponse.ok(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<Employee> getEmployeeById(@PathVariable Long id) {
        Employee employee = employeeMapper.findById(id);
        if (employee != null) {
            return ApiResponse.ok(employee);
        }
        return ApiResponse.error(HttpStatus.NOT_FOUND.value(), "Employee not found");
    }

    private byte[] buildEmployeeWorkbook(List<Employee> employees) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        EasyExcel.write(outputStream)
                .head(employeeExcelHead())
                .sheet("Employees")
                .doWrite(employeeExcelRows(employees));
        return outputStream.toByteArray();
    }

    private List<List<String>> employeeExcelHead() {
        return Arrays.asList(
                Collections.singletonList("empId"),
                Collections.singletonList("name"),
                Collections.singletonList("gender"),
                Collections.singletonList("age"),
                Collections.singletonList("position"),
                Collections.singletonList("deptId"),
                Collections.singletonList("deptName"),
                Collections.singletonList("salary"),
                Collections.singletonList("status"),
                Collections.singletonList("phone"),
                Collections.singletonList("email"),
                Collections.singletonList("joinDate")
        );
    }

    private List<List<String>> employeeExcelRows(List<Employee> employees) {
        List<List<String>> rows = new ArrayList<>();
        for (Employee employee : employees) {
            rows.add(Arrays.asList(
                    excelValue(employee.getEmpId()),
                    excelValue(employee.getName()),
                    excelValue(employee.getGender()),
                    excelValue(employee.getAge()),
                    excelValue(employee.getPosition()),
                    excelValue(employee.getDeptId()),
                    excelValue(employee.getDeptName()),
                    excelValue(employee.getSalary()),
                    excelValue(employee.getStatus()),
                    excelValue(employee.getPhone()),
                    excelValue(employee.getEmail()),
                    excelValue(employee.getJoinDate())
            ));
        }
        return rows;
    }

    private List<Employee> parseEmployeeFile(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (filename.endsWith(".csv")) {
            return parseEmployeeCsv(file);
        }
        return parseEmployeeExcel(file);
    }

    private List<Employee> parseEmployeeExcel(MultipartFile file) throws Exception {
        List<Map<Integer, String>> rows = EasyExcel.read(file.getInputStream())
                .sheet()
                .headRowNumber(1)
                .doReadSync();
        List<Employee> employees = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Map<Integer, String> row : rows) {
            List<String> columns = new ArrayList<>();
            for (int i = 0; i < 11; i++) {
                columns.add(row.get(i));
            }
            Employee employee = employeeFromColumns(columns, now);
            if (employee != null) {
                employees.add(employee);
            }
        }
        return employees;
    }

    private List<Employee> parseEmployeeCsv(MultipartFile file) throws Exception {
        String content = new String(file.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        content = content.replace("\uFEFF", "");
        String[] lines = content.split("\\r?\\n");
        List<Employee> employees = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 1; i < lines.length; i++) {
            if (lines[i].trim().isEmpty()) {
                continue;
            }
            String[] columns = lines[i].split(",", -1);
            if (columns.length < 11) {
                continue;
            }

            Employee employee = employeeFromColumns(Arrays.asList(columns), now);
            if (employee != null) {
                employees.add(employee);
            }
        }
        return employees;
    }

    private Employee employeeFromColumns(List<String> columns, LocalDateTime now) {
        if (columns.size() < 11 || blankToNull(columns.get(0)) == null) {
            return null;
        }
        Employee employee = new Employee();
        employee.setEmpId(blankToNull(columns.get(0)));
        employee.setName(blankToNull(columns.get(1)));
        employee.setGender(blankToNull(columns.get(2)));
        employee.setAge(parseInteger(columns.get(3)));
        employee.setPosition(blankToNull(columns.get(4)));
        employee.setDeptId(parseLong(columns.get(5)));
        employee.setSalary(parseBigDecimal(columns.get(6)));
        employee.setStatus(blankToNull(columns.get(7)));
        employee.setPhone(blankToNull(columns.get(8)));
        employee.setEmail(blankToNull(columns.get(9)));
        employee.setJoinDate(parseLocalDate(columns.get(10)));
        employee.setCreateTime(now);
        employee.setUpdateTime(now);
        return employee;
    }

    private String excelValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String blankToNull(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Integer parseInteger(String value) {
        String trimmed = blankToNull(value);
        return trimmed == null ? null : Integer.valueOf(trimmed);
    }

    private Long parseLong(String value) {
        String trimmed = blankToNull(value);
        return trimmed == null ? null : Long.valueOf(trimmed);
    }

    private BigDecimal parseBigDecimal(String value) {
        String trimmed = blankToNull(value);
        return trimmed == null ? null : new BigDecimal(trimmed);
    }

    private LocalDate parseLocalDate(String value) {
        String trimmed = blankToNull(value);
        return trimmed == null ? null : LocalDate.parse(trimmed);
    }
}
