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
