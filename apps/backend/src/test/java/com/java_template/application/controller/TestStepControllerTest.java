package com.java_template.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.StepReplaceDTO;
import com.java_template.application.dto.TestStepDTO;
import com.java_template.application.service.TestStepService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TestStepController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
public class TestStepControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TestStepService testStepService;

    @Test
    public void replaceSteps_returns200WithCreatedSteps() throws Exception {
        UUID projectId = UUID.randomUUID();
        UUID suiteId   = UUID.randomUUID();
        UUID caseId    = UUID.randomUUID();

        StepReplaceDTO dto = new StepReplaceDTO();
        dto.setStepNumber(1);
        dto.setAction("click login");
        dto.setExpectedResult("user logged in");

        TestStepDTO created = new TestStepDTO();
        created.setId(UUID.randomUUID());
        created.setTestCaseId(caseId);
        created.setStepNumber(1);
        created.setAction("click login");
        created.setExpectedResult("user logged in");

        when(testStepService.replaceSteps(eq(caseId), anyList()))
                .thenReturn(List.of(created));

        mockMvc.perform(put("/projects/{p}/suites/{s}/cases/{c}/steps/replace",
                        projectId, suiteId, caseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(dto))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("click login"));
    }
}
