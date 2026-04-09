package com.java_template.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.ProjectDTO;
import com.java_template.application.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.java_template.common.dto.PageResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.DisplayName;

/**
 * Unit tests for ProjectController
 */
@WebMvcTest(controllers = ProjectController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
public class ProjectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProjectService projectService;

    @Test
    public void testCreateProject() throws Exception {
        ProjectDTO project = new ProjectDTO();
        project.setName("Test Project");
        project.setDescription("A test project");

        ProjectDTO created = new ProjectDTO();
        created.setId(UUID.randomUUID());
        created.setName("Test Project");

        when(projectService.createProject(any(ProjectDTO.class))).thenReturn(created);

        mockMvc.perform(post("/projects")
                .requestAttr("role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(project)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Test Project"));
    }

    @Test
    public void testGetAllProjects() throws Exception {
        PageResult<ProjectDTO> emptyPage = PageResult.of(null, List.of(), 0, 20, 0);
        when(projectService.getAllProjects(0, 20)).thenReturn(emptyPage);

        mockMvc.perform(get("/projects")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    public void testGetProjectNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        when(projectService.getProjectById(nonExistentId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/projects/" + nonExistentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("FR 1.3: Should successfully update project")
    public void testUpdateProject() throws Exception {
        UUID projectId = UUID.randomUUID();
        ProjectDTO toUpdate = new ProjectDTO();
        toUpdate.setName("Updated Project");
        toUpdate.setDescription("Updated description");

        ProjectDTO updated = new ProjectDTO();
        updated.setId(projectId);
        updated.setName("Updated Project");
        updated.setDescription("Updated description");

        when(projectService.projectExists(projectId)).thenReturn(true);
        when(projectService.updateProject(eq(projectId), any(ProjectDTO.class))).thenReturn(updated);

        mockMvc.perform(put("/projects/" + projectId)
                .requestAttr("role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(toUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId.toString()))
                .andExpect(jsonPath("$.name").value("Updated Project"))
                .andExpect(jsonPath("$.description").value("Updated description"));
    }

    @Test
    @DisplayName("FR 1.3: Should delete project (Admin only)")
    public void testDeleteProject() throws Exception {
        UUID projectId = UUID.randomUUID();

        when(projectService.deleteProject(projectId)).thenReturn(true);

        mockMvc.perform(delete("/projects/" + projectId)
                .requestAttr("role", "ADMIN"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("FR 1.5: Tester should not be able to delete project")
    public void testDeleteProjectForbiddenForTester() throws Exception {
        UUID projectId = UUID.randomUUID();

        mockMvc.perform(delete("/projects/" + projectId)
                .requestAttr("role", "TESTER"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FR 1.3: Should get project by ID")
    public void testGetProjectById() throws Exception {
        UUID projectId = UUID.randomUUID();
        ProjectDTO project = new ProjectDTO();
        project.setId(projectId);
        project.setName("Test Project");
        project.setDescription("Test Description");

        when(projectService.getProjectById(projectId)).thenReturn(Optional.of(project));

        mockMvc.perform(get("/projects/" + projectId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId.toString()))
                .andExpect(jsonPath("$.name").value("Test Project"))
                .andExpect(jsonPath("$.description").value("Test Description"));
    }

    @Test
    @DisplayName("FR 1.3: Project name is required")
    public void testCreateProjectWithoutName() throws Exception {
        ProjectDTO project = new ProjectDTO();
        project.setDescription("Project without name");

        mockMvc.perform(post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(project)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("FR 1.3: Project name max length validation (255 chars)")
    public void testCreateProjectWithNameExceedsMaxLength() throws Exception {
        ProjectDTO project = new ProjectDTO();
        project.setName("a".repeat(256)); // Exceeds 255 char limit
        project.setDescription("Valid description");

        mockMvc.perform(post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(project)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("FR 1.3: Project description max length validation (1000 chars)")
    public void testCreateProjectWithDescriptionExceedsMaxLength() throws Exception {
        ProjectDTO project = new ProjectDTO();
        project.setName("Valid Project Name");
        project.setDescription("a".repeat(1001)); // Exceeds 1000 char limit

        mockMvc.perform(post("/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(project)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("FR 1.3: Should search projects by name")
    public void testSearchProjects() throws Exception {
        UUID projectId = UUID.randomUUID();
        ProjectDTO project = new ProjectDTO();
        project.setId(projectId);
        project.setName("Search Project");

        when(projectService.searchProjects("Search")).thenReturn(List.of(project));

        mockMvc.perform(get("/projects/search")
                .param("query", "Search")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Search Project"));
    }

    @Test
    @DisplayName("FR 1.3 + 1.5: Create project requires Admin role")
    public void testCreateProjectRequiresAdminRole() throws Exception {
        ProjectDTO project = new ProjectDTO();
        project.setName("Test");

        mockMvc.perform(post("/projects")
                .requestAttr("role", "TESTER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(project)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("FR 1.3 + 1.5: Update project requires Admin role")
    public void testUpdateProjectRequiresAdminRole() throws Exception {
        UUID projectId = UUID.randomUUID();
        ProjectDTO project = new ProjectDTO();
        project.setName("Updated");

        mockMvc.perform(put("/projects/" + projectId)
                .requestAttr("role", "TESTER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(project)))
                .andExpect(status().isForbidden());
    }
}

