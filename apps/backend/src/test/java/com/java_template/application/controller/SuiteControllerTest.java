package com.java_template.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.SuiteDTO;
import com.java_template.application.service.SuiteService;
import com.java_template.common.dto.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for SuiteController - FR 2.1, 2.13
 */
@WebMvcTest(controllers = SuiteController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
public class SuiteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SuiteService suiteService;

    private SuiteDTO suite;
    private UUID suiteId;
    private UUID projectId;

    @BeforeEach
    public void setUp() {
        suiteId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        suite = new SuiteDTO();
        suite.setId(suiteId);
        suite.setProjectId(projectId);
        suite.setName("Test Suite");
        suite.setDescription("A test suite");
    }

    @Test
    @DisplayName("POST /projects/{id}/suites - Create suite")
    public void testCreateSuite() throws Exception {
        SuiteDTO newSuite = new SuiteDTO();
        newSuite.setProjectId(projectId);
        newSuite.setName("New Suite");
        newSuite.setDescription("A new suite");

        when(suiteService.createSuite(any(SuiteDTO.class))).thenReturn(suite);

        mockMvc.perform(post("/projects/" + projectId + "/suites")
                .requestAttr("role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newSuite)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(suiteId.toString()))
                .andExpect(jsonPath("$.name").value("Test Suite"));
    }

    @Test
    @DisplayName("GET /projects/{id}/suites - Get all suites")
    public void testGetSuitesByProjectId() throws Exception {
        PageResult<SuiteDTO> page = PageResult.of(null, List.of(suite), 0, 20, 1);

        when(suiteService.getSuitesByProjectId(eq(projectId), any(Integer.class), any(Integer.class)))
                .thenReturn(page);

        mockMvc.perform(get("/projects/" + projectId + "/suites")
                .param("page", "0")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(suiteId.toString()))
                .andExpect(jsonPath("$.data[0].name").value("Test Suite"));
    }

    @Test
    @DisplayName("GET /projects/{id}/suites/{id} - Get suite by ID")
    public void testGetSuiteById() throws Exception {
        when(suiteService.getSuiteById(eq(suiteId))).thenReturn(Optional.of(suite));

        mockMvc.perform(get("/projects/" + projectId + "/suites/" + suiteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(suiteId.toString()))
                .andExpect(jsonPath("$.name").value("Test Suite"));
    }

    @Test
    @DisplayName("PUT /suites/{id} — ADMIN role — returns 200")
    public void testUpdateSuite() throws Exception {
        SuiteDTO update = new SuiteDTO();
        update.setName("Updated");

        when(suiteService.suiteExists(suiteId)).thenReturn(true);
        when(suiteService.updateSuite(eq(suiteId), any(SuiteDTO.class))).thenReturn(suite);

        mockMvc.perform(put("/projects/" + projectId + "/suites/" + suiteId)
                .requestAttr("role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(suiteId.toString()));
    }

    @Test
    @DisplayName("DELETE /suites/{id} — ADMIN role — returns 204")
    public void testDeleteSuite() throws Exception {
        when(suiteService.deleteSuite(suiteId)).thenReturn(true);

        mockMvc.perform(delete("/projects/" + projectId + "/suites/" + suiteId)
                .requestAttr("role", "ADMIN"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /suites — TESTER role — returns 403")
    public void testCreateSuiteRequiresAdminRole() throws Exception {
        SuiteDTO newSuite = new SuiteDTO();
        newSuite.setName("Should Fail");

        mockMvc.perform(post("/projects/" + projectId + "/suites")
                .requestAttr("role", "TESTER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newSuite)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /suites/{id} — TESTER role — returns 403")
    public void testUpdateSuiteRequiresAdminRole() throws Exception {
        SuiteDTO update = new SuiteDTO();
        update.setName("Updated");

        mockMvc.perform(put("/projects/" + projectId + "/suites/" + suiteId)
                .requestAttr("role", "TESTER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /suites/{id} — TESTER role — returns 403")
    public void testDeleteSuiteRequiresAdminRole() throws Exception {
        mockMvc.perform(delete("/projects/" + projectId + "/suites/" + suiteId)
                .requestAttr("role", "TESTER"))
                .andExpect(status().isForbidden());
    }

}
