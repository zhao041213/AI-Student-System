package com.ikunmanager.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CampusMcpServerRunnerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

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

    @Test
    void runnerOnlyLoadsForMcpProfile() {
        Profile profile = CampusMcpServerRunner.class.getAnnotation(Profile.class);

        assertThat(profile).isNotNull();
        assertThat(profile.value()).containsExactly("mcp");
    }

    @Test
    void mcpProfileDisablesWebServerAndBanner() throws Exception {
        java.util.Properties properties = new java.util.Properties();
        try (InputStream inputStream = getClass().getResourceAsStream("/application-mcp.properties")) {
            assertThat(inputStream).isNotNull();
            properties.load(inputStream);
        }

        assertThat(properties.getProperty("spring.main.web-application-type")).isEqualTo("none");
        assertThat(properties.getProperty("spring.main.banner-mode")).isEqualTo("off");
    }

    @Test
    void mcpLogbackProfileUsesFileAppenderWithoutConsoleAppender() throws Exception {
        Document document;
        try (InputStream inputStream = getClass().getResourceAsStream("/logback-spring.xml")) {
            assertThat(inputStream).isNotNull();
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
        }

        Element mcpProfile = springProfile(document, "mcp");
        assertThat(mcpProfile).isNotNull();
        assertThat(mcpProfile.getTextContent()).contains("logs/mcp-server.log");

        Set<String> appenderRefs = appenderRefs(mcpProfile);
        assertThat(appenderRefs).contains("MCP_FILE");
        assertThat(appenderRefs).doesNotContain("CONSOLE");
    }

    @Test
    void toolInputSchemasAreValidJsonObjects() throws Exception {
        for (CampusMcpServerRunner.ToolSpecification specification : CampusMcpServerRunner.toolSpecifications()) {
            JsonNode schema = objectMapper.readTree(specification.getInputSchema());

            assertThat(schema.path("type").asText())
                    .as(specification.getName())
                    .isEqualTo("object");
            assertThat(schema.path("properties").isObject())
                    .as(specification.getName())
                    .isTrue();
        }
    }

    private Element springProfile(Document document, String name) {
        NodeList profiles = document.getElementsByTagName("springProfile");
        for (int i = 0; i < profiles.getLength(); i++) {
            Element element = (Element) profiles.item(i);
            if (name.equals(element.getAttribute("name"))) {
                return element;
            }
        }
        return null;
    }

    private Set<String> appenderRefs(Element element) {
        NodeList appenderRefs = element.getElementsByTagName("appender-ref");
        return java.util.stream.IntStream.range(0, appenderRefs.getLength())
                .mapToObj(i -> ((Element) appenderRefs.item(i)).getAttribute("ref"))
                .collect(Collectors.toSet());
    }
}
