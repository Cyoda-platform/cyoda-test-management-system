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

import com.java_template.application.dto.*;
import com.java_template.common.dto.PageResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

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
    @DisplayName("seed skips if ANY projects already exist (not just the demo project)")
    void seed_skipsIfAnyProjectsExist() {
        // Cyoda already has a user-created project (not the demo project)
        ProjectDTO userProject = new ProjectDTO();
        userProject.setId(UUID.randomUUID());
        userProject.setName("My Real Project");
        PageResult<ProjectDTO> nonEmpty = PageResult.of(null, List.of(userProject), 0, 1, 1L);
        when(projectService.getAllProjects(anyInt(), anyInt())).thenReturn(nonEmpty);

        Map<String, Object> result = service.seed();

        assertEquals("skipped", result.get("status"));
        verify(projectService, never()).createProject(any());
    }

    @Test
    @DisplayName("seed runs if Cyoda is completely empty (no projects at all)")
    void seed_runsIfCyodaIsEmpty() {
        PageResult<ProjectDTO> empty = PageResult.of(null, List.of(), 0, 1, 0L);
        when(projectService.getAllProjects(anyInt(), anyInt())).thenReturn(empty);

        ProjectDTO created = new ProjectDTO();
        created.setId(UUID.randomUUID());
        created.setName("E-commerce Platform");
        when(projectService.createProject(any())).thenReturn(created);
        lenient().when(suiteService.createSuite(any())).thenAnswer(inv -> { SuiteDTO s = new SuiteDTO(); s.setId(UUID.randomUUID()); return s; });
        lenient().when(testCaseService.createTestCase(any())).thenAnswer(inv -> { TestCaseDTO tc = new TestCaseDTO(); tc.setId(UUID.randomUUID()); tc.setDisplayId("TC-1"); return tc; });
        // testRunService returns null — seed handles it gracefully (partial result)
        lenient().when(testRunService.createTestRun(any())).thenThrow(new RuntimeException("Cyoda unavailable"));

        service.seed();

        verify(projectService).createProject(any());
    }

    @Test
    @DisplayName("transient createTestRun failure does not abort the entire seed")
    void seed_createTestRunFailure_doesNotAbortSeed() {
        // Cyoda is empty — seed should proceed
        PageResult<ProjectDTO> empty = PageResult.of(null, List.of(), 0, 1, 0L);
        lenient().when(projectService.getAllProjects(anyInt(), anyInt())).thenReturn(empty);

        ProjectDTO project = new ProjectDTO();
        project.setId(UUID.randomUUID());
        project.setName("E-commerce Platform");
        when(projectService.createProject(any())).thenReturn(project);

        when(suiteService.createSuite(any())).thenAnswer(inv -> {
            SuiteDTO s = new SuiteDTO();
            s.setId(UUID.randomUUID());
            return s;
        });

        when(testCaseService.createTestCase(any())).thenAnswer(inv -> {
            TestCaseDTO tc = new TestCaseDTO();
            tc.setId(UUID.randomUUID());
            return tc;
        });

        // Simulate a transient Cyoda failure on TestRun creation
        doThrow(new RuntimeException("Cyoda transient failure"))
                .when(testRunService).createTestRun(any());

        // seed() must complete without propagating the exception
        assertDoesNotThrow(() -> service.seed(),
                "transient createTestRun failure must not abort the entire seed");

        verify(projectService).createProject(any());
        verify(testCaseService, atLeastOnce()).createTestCase(any());
    }

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