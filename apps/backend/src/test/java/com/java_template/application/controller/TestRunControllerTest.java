package com.java_template.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.TestRunCaseDTO;
import com.java_template.application.dto.TestRunDTO;
import com.java_template.application.dto.TestRunDetailDTO;
import com.java_template.application.service.TestRunCaseService;
import com.java_template.application.service.TestRunService;
import com.java_template.common.dto.PageResult;
import jakarta.validation.Validation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("TestRunController Tests")
class TestRunControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private AutoCloseable autoCloseable;

    @Mock private TestRunService testRunService;
    @Mock private TestRunCaseService testRunCaseService;

    private UUID projectId;
    private UUID runId;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        projectId = UUID.randomUUID();
        runId = UUID.randomUUID();
        TestRunController controller = new TestRunController(testRunService, testRunCaseService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setValidator(new SpringValidatorAdapter(
                        Validation.buildDefaultValidatorFactory().getValidator()))
                .build();
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    @DisplayName("POST valid run returns 201")
    void createTestRun_validInput_returns201() throws Exception {
        TestRunDTO dto = buildRun("My Run");
        when(testRunService.createTestRun(any(TestRunDTO.class))).thenReturn(dto);

        mockMvc.perform(post("/projects/{pid}/runs", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("My Run"));
    }

    @Test
    @DisplayName("POST run with blank name returns 400")
    void createTestRun_blankName_returns400() throws Exception {
        TestRunDTO dto = buildRun("");

        mockMvc.perform(post("/projects/{pid}/runs", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());

        verify(testRunService, never()).createTestRun(any());
    }

    @Test
    @DisplayName("GET runs returns 200 with page")
    void getTestRunsByProject_returns200WithPage() throws Exception {
        PageResult<TestRunDTO> page = PageResult.of(null, List.of(buildRun("R1")), 0, 20, 1);
        when(testRunService.getTestRunsByProjectId(eq(projectId), eq(0), eq(20))).thenReturn(page);

        mockMvc.perform(get("/projects/{pid}/runs", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /{id} — found, same project — returns 200")
    void getTestRun_found_sameProject_returns200() throws Exception {
        TestRunDTO dto = buildRun("R1");
        dto.setProjectId(projectId);
        when(testRunService.getTestRunById(eq(runId))).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/projects/{pid}/runs/{id}", projectId, runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("R1"));
    }

    @Test
    @DisplayName("GET /{id} — project ID mismatch — returns 404")
    void getTestRun_projectMismatch_returns404() throws Exception {
        TestRunDTO dto = buildRun("R1");
        dto.setProjectId(UUID.randomUUID());
        when(testRunService.getTestRunById(eq(runId))).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/projects/{pid}/runs/{id}", projectId, runId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /{id} — not found — returns 404")
    void getTestRun_notFound_returns404() throws Exception {
        when(testRunService.getTestRunById(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/projects/{pid}/runs/{id}", projectId, runId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /{id}/details — found — returns 200 with run and cases")
    void getTestRunDetails_found_returns200WithCases() throws Exception {
        TestRunDTO dto = buildRun("R1");
        dto.setProjectId(projectId);
        PageResult<TestRunCaseDTO> cases = PageResult.of(null, List.of(new TestRunCaseDTO()), 0, 500, 1);
        when(testRunService.getTestRunById(eq(runId))).thenReturn(Optional.of(dto));
        when(testRunCaseService.getTestRunCasesByTestRunId(eq(runId), eq(0), eq(500))).thenReturn(cases);

        mockMvc.perform(get("/projects/{pid}/runs/{id}/details", projectId, runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.run.name").value("R1"))
                .andExpect(jsonPath("$.runCases").isArray());
    }

    @Test
    @DisplayName("GET /{id}/details — not found — returns 404")
    void getTestRunDetails_notFound_returns404() throws Exception {
        when(testRunService.getTestRunById(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/projects/{pid}/runs/{id}/details", projectId, runId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /{id} — run exists — returns 200")
    void updateTestRun_exists_returns200() throws Exception {
        TestRunDTO dto = buildRun("Updated Run");
        when(testRunService.testRunExists(eq(runId))).thenReturn(true);
        when(testRunService.updateTestRun(eq(runId), any(TestRunDTO.class))).thenReturn(dto);

        mockMvc.perform(put("/projects/{pid}/runs/{id}", projectId, runId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Run"));
    }

    @Test
    @DisplayName("PUT /{id} — run not found — returns 404")
    void updateTestRun_notFound_returns404() throws Exception {
        when(testRunService.testRunExists(eq(runId))).thenReturn(false);

        mockMvc.perform(put("/projects/{pid}/runs/{id}", projectId, runId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRun("X"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /{id}/complete — found — returns 200")
    void completeTestRun_found_returns200() throws Exception {
        TestRunDTO dto = buildRun("R1");
        dto.setProjectId(projectId);
        dto.setStatus("COMPLETED");
        when(testRunService.getTestRunById(eq(runId))).thenReturn(Optional.of(dto));
        when(testRunService.completeTestRun(eq(runId))).thenReturn(Optional.of(dto));

        mockMvc.perform(post("/projects/{pid}/runs/{id}/complete", projectId, runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /{id}/complete — not found — returns 404")
    void completeTestRun_notFound_returns404() throws Exception {
        when(testRunService.getTestRunById(any())).thenReturn(Optional.empty());

        mockMvc.perform(post("/projects/{pid}/runs/{id}/complete", projectId, runId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /{id}/unlock — Admin role — returns 200")
    void unlockTestRun_adminRole_returns200() throws Exception {
        TestRunDTO dto = buildRun("R1");
        dto.setProjectId(projectId);
        when(testRunService.getTestRunById(eq(runId))).thenReturn(Optional.of(dto));
        when(testRunService.unlockTestRun(eq(runId))).thenReturn(Optional.of(dto));

        mockMvc.perform(post("/projects/{pid}/runs/{id}/unlock", projectId, runId)
                        .requestAttr("role", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /{id}/unlock — Tester role — returns 200")
    void unlockTestRun_testerRole_returns200() throws Exception {
        TestRunDTO dto = buildRun("R1");
        dto.setProjectId(projectId);
        when(testRunService.getTestRunById(eq(runId))).thenReturn(Optional.of(dto));
        when(testRunService.unlockTestRun(eq(runId))).thenReturn(Optional.of(dto));

        mockMvc.perform(post("/projects/{pid}/runs/{id}/unlock", projectId, runId)
                        .requestAttr("role", "TESTER"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /{id}/unlock — no role — returns 403")
    void unlockTestRun_noRole_returns403() throws Exception {
        mockMvc.perform(post("/projects/{pid}/runs/{id}/unlock", projectId, runId))
                .andExpect(status().isForbidden());

        verify(testRunService, never()).unlockTestRun(any());
    }

    @Test
    @DisplayName("DELETE /{id} — exists — returns 204")
    void deleteTestRun_exists_returns204() throws Exception {
        when(testRunService.deleteTestRun(eq(runId))).thenReturn(true);

        mockMvc.perform(delete("/projects/{pid}/runs/{id}", projectId, runId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /{id} — not found — returns 404")
    void deleteTestRun_notFound_returns404() throws Exception {
        when(testRunService.deleteTestRun(eq(runId))).thenReturn(false);

        mockMvc.perform(delete("/projects/{pid}/runs/{id}", projectId, runId))
                .andExpect(status().isNotFound());
    }

    private TestRunDTO buildRun(String name) {
        TestRunDTO dto = new TestRunDTO();
        dto.setId(runId);
        dto.setProjectId(projectId);
        dto.setName(name);
        dto.setStatus("ACTIVE");
        return dto;
    }
}
