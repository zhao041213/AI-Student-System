package com.ikunmanager.controller;

import com.alibaba.excel.EasyExcel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ikunmanager.entity.Employee;
import com.ikunmanager.mapper.EmployeeMapper;
import com.ikunmanager.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmployeeMapper employeeMapper;

    @MockBean
    private EmployeeService employeeService;

    @Test
    void addEmployeeCreatesEmployee() throws Exception {
        Employee employee = sampleEmployee();
        when(employeeMapper.insert(any(Employee.class))).thenAnswer(invocation -> {
            Employee inserted = invocation.getArgument(0);
            inserted.setId(10L);
            return 1;
        });
        when(employeeMapper.findById(10L)).thenReturn(employee);

        mockMvc.perform(post("/api/employee/add")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.emp_id", is("EMP999")));

        verify(employeeMapper).insert(any(Employee.class));
    }

    @Test
    void updateEmployeeUpdatesExistingEmployee() throws Exception {
        Employee employee = sampleEmployee();
        employee.setId(1L);
        when(employeeMapper.findById(1L)).thenReturn(employee);
        when(employeeMapper.update(any(Employee.class))).thenReturn(1);

        mockMvc.perform(put("/api/employee/{id}", 1L)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.id", is(1)));

        verify(employeeMapper).update(any(Employee.class));
    }

    @Test
    void deleteEmployeeDeletesExistingEmployee() throws Exception {
        Employee employee = sampleEmployee();
        employee.setId(1L);
        when(employeeMapper.findById(1L)).thenReturn(employee);
        when(employeeMapper.delete(1L)).thenReturn(1);

        mockMvc.perform(delete("/api/employee/{id}", 1L)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)));

        verify(employeeMapper).delete(1L);
    }

    @Test
    void batchDeleteEmployeesDeletesIds() throws Exception {
        when(employeeMapper.batchDelete(anyList())).thenReturn(2);

        mockMvc.perform(delete("/api/employee/batch")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[1,2]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)));

        verify(employeeMapper).batchDelete(anyList());
    }

    @Test
    void exportEmployeesReturnsExcel() throws Exception {
        when(employeeMapper.findAll(null, null, null, null))
                .thenReturn(Collections.singletonList(sampleEmployee()));

        mockMvc.perform(get("/api/employee/export")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("employees.xlsx")))
                .andExpect(content().contentTypeCompatibleWith(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    void importEmployeesReadsCsvRows() throws Exception {
        when(employeeMapper.batchInsert(anyList())).thenReturn(1);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "employees.csv",
                "text/csv",
                ("empId,name,gender,age,position,deptId,salary,status,phone,email,joinDate\n" +
                        "EMP998,Imported Employee,M,26,QA,1,9000.00,active,13900000001,import@example.com,2026-06-02\n")
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/employee/import")
                        .file(file)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.imported", is(1)));

        verify(employeeMapper).batchInsert(anyList());
    }

    @Test
    void importEmployeesReadsExcelRows() throws Exception {
        when(employeeMapper.batchInsert(anyList())).thenReturn(1);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        EasyExcel.write(outputStream)
                .head(employeeExcelHead())
                .sheet("Employees")
                .doWrite(Collections.singletonList(Arrays.asList(
                        "EMP997",
                        "Excel Employee",
                        "M",
                        "27",
                        "QA",
                        "1",
                        "9100.00",
                        "active",
                        "13900000002",
                        "excel@example.com",
                        "2026-06-02"
                )));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "employees.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                outputStream.toByteArray()
        );

        mockMvc.perform(multipart("/api/employee/import")
                        .file(file)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.data.imported", is(1)));

        verify(employeeMapper).batchInsert(anyList());
    }

    private Employee sampleEmployee() {
        Employee employee = new Employee();
        employee.setId(10L);
        employee.setEmpId("EMP999");
        employee.setName("Test Employee");
        employee.setGender("M");
        employee.setAge(25);
        employee.setPosition("QA");
        employee.setDeptId(1L);
        employee.setDeptName("Tech");
        employee.setSalary(new BigDecimal("9000.00"));
        employee.setStatus("active");
        employee.setPhone("13900000000");
        employee.setEmail("test@example.com");
        employee.setJoinDate(LocalDate.of(2026, 6, 2));
        return employee;
    }

    private List<List<String>> employeeExcelHead() {
        return Arrays.asList(
                Collections.singletonList("empId"),
                Collections.singletonList("name"),
                Collections.singletonList("gender"),
                Collections.singletonList("age"),
                Collections.singletonList("position"),
                Collections.singletonList("deptId"),
                Collections.singletonList("salary"),
                Collections.singletonList("status"),
                Collections.singletonList("phone"),
                Collections.singletonList("email"),
                Collections.singletonList("joinDate")
        );
    }
}
