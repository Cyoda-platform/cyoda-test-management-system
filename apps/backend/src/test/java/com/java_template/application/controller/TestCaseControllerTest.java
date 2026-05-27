package com.java_template.application.controller;

import com.java_template.application.dto.TestCaseDTO;
import com.java_template.application.service.TestCaseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TestCaseController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
public class TestCaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TestCaseService testCaseService;

    @Test
    public void testCreateTestCaseUsesPathVariablesInsteadOfBodyIds() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID suiteId = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();

        TestCaseDTO created = new TestCaseDTO();
        created.setId(caseId);
        created.setProjectId(projectId);
        created.setSuiteId(suiteId);
        created.setTitle("Test1");

        when(testCaseService.createTestCase(any(TestCaseDTO.class))).thenReturn(created);

        mockMvc.perform(post("/projects/{projectId}/suites/{suiteId}/cases", projectId, suiteId)
                        .requestAttr("role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Test1","priority":"MEDIUM","description":"","preconditions":""}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(caseId.toString()))
                .andExpect(jsonPath("$.projectId").value(projectId.toString()))
                .andExpect(jsonPath("$.suiteId").value(suiteId.toString()))
                .andExpect(jsonPath("$.title").value("Test1"));

        verify(testCaseService).createTestCase(argThat(testCase ->
                projectId.equals(testCase.getProjectId())
                        && suiteId.equals(testCase.getSuiteId())
                        && "Test1".equals(testCase.getTitle())));
    }

    @Test
    @DisplayName("POST /cases — TESTER role — returns 403")
    public void testCreateTestCaseRequiresAdminRole() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID suiteId = UUID.randomUUID();

        mockMvc.perform(post("/projects/{projectId}/suites/{suiteId}/cases", projectId, suiteId)
                        .requestAttr("role", "TESTER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Blocked","priority":"MEDIUM","description":"","preconditions":""}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /cases/{id} — TESTER role — returns 403")
    public void testUpdateTestCaseRequiresAdminRole() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID suiteId = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();

        mockMvc.perform(put("/projects/{projectId}/suites/{suiteId}/cases/{id}", projectId, suiteId, caseId)
                        .requestAttr("role", "TESTER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Blocked","priority":"MEDIUM","description":"","preconditions":""}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /cases/{id} — TESTER role — returns 403")
    public void testDeleteTestCaseRequiresAdminRole() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID suiteId = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();

        mockMvc.perform(delete("/projects/{projectId}/suites/{suiteId}/cases/{id}", projectId, suiteId, caseId)
                        .requestAttr("role", "TESTER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /cases?size=5000 — caps effective size at MAX_PAGE_SIZE to prevent Cyoda overload")
    public void testGetCasesCapsSizeAtMaxPageSize() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID suiteId = UUID.randomUUID();

        when(testCaseService.getTestCasesBySuiteId(any(), anyInt(), anyInt()))
                .thenReturn(com.java_template.common.dto.PageResult.of(null, java.util.List.of(), 0, 1000, 0L));

        mockMvc.perform(get("/projects/{projectId}/suites/{suiteId}/cases", projectId, suiteId)
                        .param("size", "5000"))
                .andExpect(status().isOk());

        // size must be capped — service must NOT be called with 5000
        ArgumentCaptor<Integer> sizeCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(testCaseService).getTestCasesBySuiteId(eq(suiteId), anyInt(), sizeCaptor.capture());
        assertTrue(sizeCaptor.getValue() <= 1000, "size must be capped at MAX_PAGE_SIZE");
    }

    @Test
    @DisplayName("POST /cases/batch — TESTER role — returns 403")
    public void testBatchCreateTestCasesRequiresAdminRole() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID suiteId = UUID.randomUUID();

        mockMvc.perform(post("/projects/{projectId}/suites/{suiteId}/cases/batch", projectId, suiteId)
                        .requestAttr("role", "TESTER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isForbidden());
    }
}