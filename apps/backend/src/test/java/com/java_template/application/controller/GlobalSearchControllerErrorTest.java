package com.java_template.application.controller;

import com.java_template.application.service.ProjectService;
import com.java_template.application.service.SearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GlobalSearchController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class GlobalSearchControllerErrorTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchService searchService;

    @MockBean
    private ProjectService projectService;

    // CR-03: search failure must not expose exception details in response body
    @Test
    void searchFailureReturnsGenericMessageNotExceptionDetails() throws Exception {
        String sensitiveDetail = "Bearer eyJsensitiveToken internal-secret-key-xyz";
        when(projectService.getAllProjects(anyInt(), anyInt()))
                .thenThrow(new RuntimeException(sensitiveDetail));

        mockMvc.perform(get("/v1/search").param("query", "test"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(content().string(not(containsString(sensitiveDetail))))
                .andExpect(content().string(not(containsString("eyJsensitiveToken"))));
    }
}
