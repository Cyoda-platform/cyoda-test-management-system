package com.java_template.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.AttachmentDTO;
import com.java_template.application.dto.TestRunStepDTO;
import com.java_template.application.service.AttachmentService;
import com.java_template.application.service.TestRunStepService;
import com.java_template.common.dto.PageResult;
import jakarta.validation.Validation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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

@DisplayName("TestRunStepController Tests")
class TestRunStepControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private AutoCloseable autoCloseable;

    @Mock private TestRunStepService testRunStepService;
    @Mock private AttachmentService attachmentService;

    private UUID projectId;
    private UUID runId;
    private UUID caseId;
    private UUID stepId;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        projectId = UUID.randomUUID();
        runId = UUID.randomUUID();
        caseId = UUID.randomUUID();
        stepId = UUID.randomUUID();
        mockMvc = MockMvcBuilders.standaloneSetup(
                new TestRunStepController(testRunStepService, attachmentService))
                .setValidator(new SpringValidatorAdapter(
                        Validation.buildDefaultValidatorFactory().getValidator()))
                .build();
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    @DisplayName("POST creates step and returns 201")
    void createTestRunStep_returns201() throws Exception {
        TestRunStepDTO dto = buildStep();
        when(testRunStepService.createTestRunStep(any(TestRunStepDTO.class))).thenReturn(dto);

        mockMvc.perform(post("/projects/{pid}/runs/{rid}/cases/{cid}/steps",
                        projectId, runId, caseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("GET steps returns 200 with page")
    void getTestRunStepsByCase_returns200() throws Exception {
        PageResult<TestRunStepDTO> page = PageResult.of(null, List.of(buildStep()), 0, 20, 1);
        when(testRunStepService.getTestRunStepsByTestRunCaseId(eq(caseId), eq(0), eq(20)))
                .thenReturn(page);

        mockMvc.perform(get("/projects/{pid}/runs/{rid}/cases/{cid}/steps",
                        projectId, runId, caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /{id} — found, same case — returns 200")
    void getTestRunStep_found_sameCase_returns200() throws Exception {
        TestRunStepDTO dto = buildStep();
        dto.setTestRunCaseId(caseId);
        when(testRunStepService.getTestRunStepById(eq(stepId))).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/projects/{pid}/runs/{rid}/cases/{cid}/steps/{id}",
                        projectId, runId, caseId, stepId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /{id} — caseId mismatch — returns 404")
    void getTestRunStep_caseMismatch_returns404() throws Exception {
        TestRunStepDTO dto = buildStep();
        dto.setTestRunCaseId(UUID.randomUUID());
        when(testRunStepService.getTestRunStepById(eq(stepId))).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/projects/{pid}/runs/{rid}/cases/{cid}/steps/{id}",
                        projectId, runId, caseId, stepId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /{id} — found — updates from body and returns 200")
    void updateTestRunStep_found_returns200() throws Exception {
        TestRunStepDTO dto = buildStep();
        dto.setStatus("PASSED");
        when(testRunStepService.updateTestRunStepStatus(eq(stepId), eq("PASSED")))
                .thenReturn(Optional.of(dto));

        mockMvc.perform(put("/projects/{pid}/runs/{rid}/cases/{cid}/steps/{id}",
                        projectId, runId, caseId, stepId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /{id} — not found — returns 404")
    void updateTestRunStep_notFound_returns404() throws Exception {
        TestRunStepDTO dto = buildStep();
        dto.setStatus("PASSED");
        when(testRunStepService.updateTestRunStepStatus(any(), any())).thenReturn(Optional.empty());

        mockMvc.perform(put("/projects/{pid}/runs/{rid}/cases/{cid}/steps/{id}",
                        projectId, runId, caseId, stepId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /{id} — actualResult exceeds 2000 chars — returns 400")
    void updateTestRunStep_actualResultTooLong_returns400() throws Exception {
        TestRunStepDTO dto = buildStep();
        dto.setActualResult("x".repeat(2001));

        mockMvc.perform(put("/projects/{pid}/runs/{rid}/cases/{cid}/steps/{id}",
                        projectId, runId, caseId, stepId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /{id}/status — found — returns 200")
    void updateTestRunStepStatus_found_returns200() throws Exception {
        TestRunStepDTO dto = buildStep();
        dto.setStatus("FAILED");
        when(testRunStepService.updateTestRunStepStatus(eq(stepId), eq("FAILED")))
                .thenReturn(Optional.of(dto));

        mockMvc.perform(put("/projects/{pid}/runs/{rid}/cases/{cid}/steps/{id}/status",
                        projectId, runId, caseId, stepId)
                        .param("status", "FAILED"))
                .andExpect(status().isOk());
    }

    private TestRunStepDTO buildStep() {
        TestRunStepDTO dto = new TestRunStepDTO();
        dto.setId(stepId);
        dto.setTestRunCaseId(caseId);
        dto.setStatus("UNTESTED");
        dto.setActualResult("Test step result");
        return dto;
    }
}
