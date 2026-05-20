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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SG-08: SearchController must not expose a GET endpoint at the class-level path.
 * Global search belongs exclusively in GlobalSearchController (/v1/search).
 * SearchController is per-project only: POST /v1/projects/{id}/search and
 * GET /v1/projects/{id}/search/quick.
 */
@WebMvcTest(controllers = SearchController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class SearchControllerDuplicationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchService searchService;

    @MockBean
    private ProjectService projectService;

    @Test
    void getOnProjectSearchPath_returns405MethodNotAllowed() throws Exception {
        // GET /v1/projects/{id}/search must not be handled by SearchController.
        // Only POST (search) and GET /quick are valid.
        mockMvc.perform(get("/v1/projects/{projectId}/search", UUID.randomUUID())
                        .param("query", "anything"))
                .andExpect(status().isMethodNotAllowed());
    }
}
