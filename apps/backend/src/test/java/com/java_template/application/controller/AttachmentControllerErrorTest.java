package com.java_template.application.controller;

import com.java_template.application.service.AttachmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AttachmentController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class AttachmentControllerErrorTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AttachmentService attachmentService;

    // CR-03: upload failure must not expose exception details
    @Test
    void uploadFailureReturnsGenericMessageNotExceptionDetails() throws Exception {
        String sensitiveDetail = "DB credentials: user=admin password=s3cr3t";
        when(attachmentService.uploadAttachment(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException(sensitiveDetail));

        UUID projectId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/projects/{projectId}/attachments", projectId).file(file))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(content().string(not(containsString(sensitiveDetail))))
                .andExpect(content().string(not(containsString("DB credentials"))));
    }

    // CR-03: copy failure must not expose exception details
    @Test
    void copyFailureReturnsGenericMessageNotExceptionDetails() throws Exception {
        String sensitiveDetail = "Internal path: /etc/secrets/cyoda-token";
        when(attachmentService.getAttachmentById(any()))
                .thenThrow(new RuntimeException(sensitiveDetail));

        UUID projectId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        UUID toCaseId = UUID.randomUUID();

        mockMvc.perform(post("/projects/{projectId}/attachments/{id}/copy", projectId, attachmentId)
                .param("toCaseId", toCaseId.toString()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(content().string(not(containsString(sensitiveDetail))))
                .andExpect(content().string(not(containsString("/etc/secrets"))));
    }
}
