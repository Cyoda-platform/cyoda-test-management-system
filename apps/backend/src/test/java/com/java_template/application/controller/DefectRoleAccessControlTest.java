package com.java_template.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.DefectDTO;
import com.java_template.application.service.DefectService;
import org.junit.jupiter.api.Test;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DefectController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class DefectRoleAccessControlTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DefectService defectService;

    // ---- CREATE ----

    @Test
    void testerCanCreateDefect() throws Exception {
        UUID projectId = UUID.randomUUID();
        DefectDTO created = defectWithProject(projectId);
        when(defectService.createDefect(any())).thenReturn(created);

        mockMvc.perform(post("/projects/{projectId}/defects", projectId)
                        .requestAttr("role", "TESTER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defectBody())))
                .andExpect(status().isCreated());
    }

    @Test
    void adminCanCreateDefect() throws Exception {
        UUID projectId = UUID.randomUUID();
        DefectDTO created = defectWithProject(projectId);
        when(defectService.createDefect(any())).thenReturn(created);

        mockMvc.perform(post("/projects/{projectId}/defects", projectId)
                        .requestAttr("role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defectBody())))
                .andExpect(status().isCreated());
    }

    @Test
    void noRoleCannotCreateDefect() throws Exception {
        UUID projectId = UUID.randomUUID();

        mockMvc.perform(post("/projects/{projectId}/defects", projectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defectBody())))
                .andExpect(status().isForbidden());
    }

    // ---- UPDATE ----

    @Test
    void testerCanUpdateDefect() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID defectId = UUID.randomUUID();
        DefectDTO updated = defectWithProject(projectId);
        when(defectService.defectExists(defectId)).thenReturn(true);
        when(defectService.updateDefect(eq(defectId), any())).thenReturn(updated);

        mockMvc.perform(put("/projects/{projectId}/defects/{id}", projectId, defectId)
                        .requestAttr("role", "TESTER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defectBody())))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanUpdateDefect() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID defectId = UUID.randomUUID();
        DefectDTO updated = defectWithProject(projectId);
        when(defectService.defectExists(defectId)).thenReturn(true);
        when(defectService.updateDefect(eq(defectId), any())).thenReturn(updated);

        mockMvc.perform(put("/projects/{projectId}/defects/{id}", projectId, defectId)
                        .requestAttr("role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defectBody())))
                .andExpect(status().isOk());
    }

    @Test
    void noRoleCannotUpdateDefect() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID defectId = UUID.randomUUID();

        mockMvc.perform(put("/projects/{projectId}/defects/{id}", projectId, defectId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(defectBody())))
                .andExpect(status().isForbidden());
    }

    // ---- DELETE ----

    @Test
    void adminCanDeleteDefect() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID defectId = UUID.randomUUID();
        when(defectService.defectExists(defectId)).thenReturn(true);

        mockMvc.perform(delete("/projects/{projectId}/defects/{id}", projectId, defectId)
                        .requestAttr("role", "ADMIN"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testerCannotDeleteDefect() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID defectId = UUID.randomUUID();

        mockMvc.perform(delete("/projects/{projectId}/defects/{id}", projectId, defectId)
                        .requestAttr("role", "TESTER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void noRoleCannotDeleteDefect() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID defectId = UUID.randomUUID();

        mockMvc.perform(delete("/projects/{projectId}/defects/{id}", projectId, defectId))
                .andExpect(status().isForbidden());
    }

    // ---- helpers ----

    private DefectDTO defectBody() {
        DefectDTO dto = new DefectDTO();
        dto.setTitle("Login button broken");
        dto.setSeverity("HIGH");
        dto.setStatus("OPEN");
        return dto;
    }

    private DefectDTO defectWithProject(UUID projectId) {
        DefectDTO dto = defectBody();
        dto.setId(UUID.randomUUID());
        dto.setProjectId(projectId);
        return dto;
    }
}
