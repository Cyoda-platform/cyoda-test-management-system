package com.java_template.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.DefectDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
@DisplayName("DemoSeederService")
class DemoSeederServiceTest {

    @Mock private ProjectService projectService;
    @Mock private SuiteService suiteService;
    @Mock private TestCaseService testCaseService;
    @Mock private TestRunService testRunService;
    @Mock private TestRunCaseService testRunCaseService;
    @Mock private TestRunStepService testRunStepService;
    @Mock private DefectService defectService;
    @Mock private AttachmentService attachmentService;
    @Mock private ReportService reportService;
    @Spy  private ObjectMapper objectMapper;

    @InjectMocks
    private DemoSeederService service;

    @Test
    @DisplayName("buildSnapshotData includes id, displayId and createdAt for defects")
    void buildSnapshotData_includesFullDefectFields() throws Exception {
        DemoSeederService.DemoData data = new DemoSeederService.DemoData();
        data.suites = List.of();

        DemoSeederService.DemoDefect demoDefect = new DemoSeederService.DemoDefect();
        demoDefect.caseRef = "case-1";
        demoDefect.stepNumber = 3;
        demoDefect.title = "Session token not invalidated";
        demoDefect.description = "desc";
        demoDefect.severity = "Critical";
        data.defects = List.of(demoDefect);

        DefectDTO createdDefect = new DefectDTO();
        createdDefect.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        createdDefect.setDisplayId("DEF-1");
        createdDefect.setTitle(demoDefect.title);
        createdDefect.setSeverity(demoDefect.severity);
        createdDefect.setStatus("Open");
        createdDefect.setSource("Step 3");
        createdDefect.setCreatedAt("2026-05-14T10:00:00Z");

        Method method = DemoSeederService.class.getDeclaredMethod(
                "buildSnapshotData",
                DemoSeederService.DemoData.class,
                int.class,
                int.class,
                int.class,
                String.class,
                String.class,
                String.class,
                Map.class,
                List.class
        );
        method.setAccessible(true);

        String json = (String) method.invoke(
                service,
                data,
                1,
                0,
                0,
                "Staging",
                "v1",
                "Run 1",
                Map.of("case-1::3", createdDefect),
                data.defects
        );

        JsonNode defect = objectMapper.readTree(json).path("defects").get(0);
        assertEquals("11111111-1111-1111-1111-111111111111", defect.path("id").asText());
        assertEquals("DEF-1", defect.path("displayId").asText());
        assertEquals("2026-05-14T10:00:00Z", defect.path("createdAt").asText());
    }
}