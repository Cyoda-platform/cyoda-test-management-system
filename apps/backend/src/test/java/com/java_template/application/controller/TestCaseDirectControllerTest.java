package com.java_template.application.controller;

import com.java_template.application.service.TestCaseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TestCaseDirectController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
public class TestCaseDirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TestCaseService testCaseService;

    @Test
    @DisplayName("DELETE /projects/{projectId}/cases/{caseId} — ADMIN role — returns 204")
    public void testDeleteTestCaseAsAdmin() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();

        when(testCaseService.deleteTestCase(caseId)).thenReturn(true);

        mockMvc.perform(delete("/projects/{projectId}/cases/{caseId}", projectId, caseId)
                        .requestAttr("role", "ADMIN"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /projects/{projectId}/cases/{caseId} — TESTER role — returns 403")
    public void testDeleteTestCaseAsTesterIsForbidden() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();

        mockMvc.perform(delete("/projects/{projectId}/cases/{caseId}", projectId, caseId)
                        .requestAttr("role", "TESTER"))
                .andExpect(status().isForbidden());
    }
}
