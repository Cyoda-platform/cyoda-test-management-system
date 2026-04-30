package com.java_template.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.TestRunCaseDTO;
import com.java_template.application.service.TestRunCaseService;
import com.java_template.common.dto.PageResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("TestRunCaseController Tests")
class TestRunCaseControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private AutoCloseable autoCloseable;

    @Mock private TestRunCaseService testRunCaseService;

    private UUID projectId;
    private UUID runId;
    private UUID caseId;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        projectId = UUID.randomUUID();
        runId = UUID.randomUUID();
        caseId = UUID.randomUUID();
        mockMvc = MockMvcBuilders.standaloneSetup(
                new TestRunCaseController(testRunCaseService)).build();
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    @DisplayName("POST creates case and returns 201")
    void createTestRunCase_returns201() throws Exception {
        TestRunCaseDTO dto = buildCase();
        when(testRunCaseService.createTestRunCase(any(TestRunCaseDTO.class))).thenReturn(dto);

        mockMvc.perform(post("/projects/{pid}/runs/{rid}/cases", projectId, runId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("GET cases returns 200 with page")
    void getTestRunCasesByRun_returns200() throws Exception {
        PageResult<TestRunCaseDTO> page = PageResult.of(null, List.of(buildCase()), 0, 20, 1);
        when(testRunCaseService.getTestRunCasesByTestRunId(eq(runId), eq(0), eq(20))).thenReturn(page);

        mockMvc.perform(get("/projects/{pid}/runs/{rid}/cases", projectId, runId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /{id} — found, same run — returns 200")
    void getTestRunCase_found_sameRun_returns200() throws Exception {
        TestRunCaseDTO dto = buildCase();
        dto.setTestRunId(runId);
        when(testRunCaseService.getTestRunCaseById(eq(caseId))).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/projects/{pid}/runs/{rid}/cases/{id}", projectId, runId, caseId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /{id} — runId mismatch — returns 404")
    void getTestRunCase_runMismatch_returns404() throws Exception {
        TestRunCaseDTO dto = buildCase();
        dto.setTestRunId(UUID.randomUUID());
        when(testRunCaseService.getTestRunCaseById(eq(caseId))).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/projects/{pid}/runs/{rid}/cases/{id}", projectId, runId, caseId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /{id} — found — updates status from body and returns 200")
    void updateTestRunCase_found_returns200() throws Exception {
        TestRunCaseDTO dto = buildCase();
        dto.setStatus("PASSED");
        when(testRunCaseService.updateTestRunCaseStatus(eq(caseId), eq("PASSED")))
                .thenReturn(Optional.of(dto));

        mockMvc.perform(put("/projects/{pid}/runs/{rid}/cases/{id}", projectId, runId, caseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /{id} — not found — returns 404")
    void updateTestRunCase_notFound_returns404() throws Exception {
        TestRunCaseDTO dto = buildCase();
        dto.setStatus("PASSED");
        when(testRunCaseService.updateTestRunCaseStatus(any(), any())).thenReturn(Optional.empty());

        mockMvc.perform(put("/projects/{pid}/runs/{rid}/cases/{id}", projectId, runId, caseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /{id}/status — found — returns 200")
    void updateTestRunCaseStatus_found_returns200() throws Exception {
        TestRunCaseDTO dto = buildCase();
        dto.setStatus("FAILED");
        when(testRunCaseService.updateTestRunCaseStatus(eq(caseId), eq("FAILED")))
                .thenReturn(Optional.of(dto));

        mockMvc.perform(put("/projects/{pid}/runs/{rid}/cases/{id}/status",
                        projectId, runId, caseId)
                        .param("status", "FAILED"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /{id}/status — not found — returns 404")
    void updateTestRunCaseStatus_notFound_returns404() throws Exception {
        when(testRunCaseService.updateTestRunCaseStatus(any(), any())).thenReturn(Optional.empty());

        mockMvc.perform(put("/projects/{pid}/runs/{rid}/cases/{id}/status",
                        projectId, runId, caseId)
                        .param("status", "FAILED"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /{id}/link-bug — found — returns 200")
    void linkBug_found_returns200() throws Exception {
        TestRunCaseDTO dto = buildCase();
        dto.setBugUrl("https://jira.example.com/BUG-123");
        when(testRunCaseService.linkBug(eq(caseId), eq("https://jira.example.com/BUG-123")))
                .thenReturn(Optional.of(dto));

        mockMvc.perform(post("/projects/{pid}/runs/{rid}/cases/{id}/link-bug",
                        projectId, runId, caseId)
                        .param("bugUrl", "https://jira.example.com/BUG-123"))
                .andExpect(status().isOk());
    }

    private TestRunCaseDTO buildCase() {
        TestRunCaseDTO dto = new TestRunCaseDTO();
        dto.setId(caseId);
        dto.setTestRunId(runId);
        dto.setProjectId(projectId);
        dto.setStatus("UNTESTED");
        dto.setTitle("Sample Case");
        return dto;
    }
}
