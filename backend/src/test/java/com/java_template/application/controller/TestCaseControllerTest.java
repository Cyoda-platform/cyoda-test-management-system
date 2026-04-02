package com.java_template.application.controller;

import com.java_template.application.dto.TestCaseDTO;
import com.java_template.application.service.TestCaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}