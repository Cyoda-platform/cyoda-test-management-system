package com.java_template.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.SearchRequestDTO;
import com.java_template.application.dto.SearchResponseDTO;
import com.java_template.application.service.ProjectService;
import com.java_template.application.service.SearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * IM-06: SearchController was registered at /api/v1/... instead of /v1/...
 * With server context-path=/api the effective URL was /api/api/v1/... (unreachable).
 * These tests verify the correct mapping /v1/projects/{id}/search.
 */
@WebMvcTest(controllers = SearchController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class SearchControllerMappingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SearchService searchService;

    @MockBean
    private ProjectService projectService;

    @Test
    void postSearchIsReachableAtCorrectPath() throws Exception {
        SearchResponseDTO response = new SearchResponseDTO();
        response.setResults(List.of());
        response.setTotalResults(0);
        when(searchService.search(any(), any(), anyInt(), anyInt())).thenReturn(response);

        SearchRequestDTO request = new SearchRequestDTO();
        request.setQuery("login");

        mockMvc.perform(post("/v1/projects/{projectId}/search", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void quickSearchIsReachableAtCorrectPath() throws Exception {
        SearchResponseDTO response = new SearchResponseDTO();
        response.setResults(List.of());
        response.setTotalResults(0);
        when(searchService.search(any(), any(), anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/v1/projects/{projectId}/search/quick", UUID.randomUUID())
                        .param("query", "login"))
                .andExpect(status().isOk());
    }
}
