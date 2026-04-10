package com.java_template.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.ProjectDTO;
import com.java_template.application.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for Role-Based Access Control (FR 1.5)
 * Verifies that Admin and Tester roles have appropriate permissions
 */
@WebMvcTest(controllers = ProjectController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
public class ProjectRoleAccessControlTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectService projectService;

    @Test
    @DisplayName("FR 1.5: Admin can create a project")
    public void testAdminCanCreateProject() throws Exception {
        ProjectDTO project = new ProjectDTO();
        project.setName("Admin Project");
        project.setDescription("Created by admin");

        ProjectDTO created = new ProjectDTO();
        created.setId(UUID.randomUUID());
        created.setName("Admin Project");

        when(projectService.createProject(any(ProjectDTO.class))).thenReturn(created);

        mockMvc.perform(post("/projects")
                .requestAttr("role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(project)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("FR 1.5: Admin can delete a project")
    public void testAdminCanDeleteProject() throws Exception {
        UUID projectId = UUID.randomUUID();

        when(projectService.deleteProject(projectId)).thenReturn(true);

        mockMvc.perform(delete("/projects/" + projectId)
                .requestAttr("role", "ADMIN"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("FR 1.5: Tester has read-only access - cannot create project")
    public void testTesterCannotCreateProject() throws Exception {
        ProjectDTO project = new ProjectDTO();
        project.setName("Tester Project");

        mockMvc.perform(post("/projects")
                .requestAttr("role", "TESTER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(project)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FR 1.5: Tester has read-only access - cannot update project")
    public void testTesterCannotUpdateProject() throws Exception {
        UUID projectId = UUID.randomUUID();
        ProjectDTO project = new ProjectDTO();
        project.setName("Updated");

        mockMvc.perform(put("/projects/" + projectId)
                .requestAttr("role", "TESTER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(project)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FR 1.5: Tester can read/view projects")
    public void testTesterCanReadProjects() throws Exception {
        mockMvc.perform(get("/projects")
                .requestAttr("role", "TESTER"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("FR 1.5: Tester can view project by ID")
    public void testTesterCanViewProjectById() throws Exception {
        UUID projectId = UUID.randomUUID();
        ProjectDTO project = new ProjectDTO();
        project.setId(projectId);
        project.setName("View Project");

        when(projectService.getProjectById(projectId)).thenReturn(Optional.of(project));

        mockMvc.perform(get("/projects/" + projectId)
                .requestAttr("role", "TESTER"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("FR 1.5: Tester cannot delete projects")
    public void testTesterCannotDeleteProject() throws Exception {
        UUID projectId = UUID.randomUUID();

        mockMvc.perform(delete("/projects/" + projectId)
                .requestAttr("role", "TESTER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FR 1.3 + 1.5: Admin has full CRUD permissions")
    public void testAdminHasFullCrudPermissions() throws Exception {
        UUID projectId = UUID.randomUUID();
        ProjectDTO project = new ProjectDTO();
        project.setName("CRUD Test");

        when(projectService.createProject(any())).thenReturn(project);
        when(projectService.projectExists(projectId)).thenReturn(true);
        when(projectService.updateProject(any(), any())).thenReturn(project);
        when(projectService.deleteProject(projectId)).thenReturn(true);

        // CREATE - should succeed
        mockMvc.perform(post("/projects")
                .requestAttr("role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(project)))
                .andExpect(status().isCreated());

        // UPDATE - should succeed
        mockMvc.perform(put("/projects/" + projectId)
                .requestAttr("role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(project)))
                .andExpect(status().isOk());

        // DELETE - should succeed
        mockMvc.perform(delete("/projects/" + projectId)
                .requestAttr("role", "ADMIN"))
                .andExpect(status().isNoContent());
    }
}
