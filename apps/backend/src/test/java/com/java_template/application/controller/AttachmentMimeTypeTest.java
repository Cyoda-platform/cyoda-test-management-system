package com.java_template.application.controller;

import com.java_template.application.service.AttachmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SG-07: AttachmentController must reject uploads with disallowed MIME types.
 */
@WebMvcTest(controllers = AttachmentController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
class AttachmentMimeTypeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AttachmentService attachmentService;

    @ParameterizedTest
    @ValueSource(strings = {
            "application/x-msdownload",   // .exe
            "application/x-sh",           // shell script
            "text/html",                  // XSS risk
            "application/javascript",     // XSS risk
            "application/x-php"           // server-side script
    })
    void uploadWithDisallowedMimeType_returns400(String mimeType) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "malicious.exe", mimeType, new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/projects/{projectId}/attachments", UUID.randomUUID()).file(file))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "image/png",
            "image/jpeg",
            "application/pdf",
            "text/plain",
            "text/csv"
    })
    void uploadWithAllowedMimeType_proceeds(String mimeType) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", mimeType, new byte[]{1, 2, 3});

        // Service not stubbed → will throw → controller catches and returns 500
        // Important: we just verify it does NOT return 400 (MIME check passed)
        mockMvc.perform(multipart("/projects/{projectId}/attachments", UUID.randomUUID()).file(file))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 400) {
                        throw new AssertionError("Expected non-400 for allowed MIME type " + mimeType + " but got 400");
                    }
                });
    }

    @Test
    void uploadWithNullContentType_returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "unknown.bin", (String) null, new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/projects/{projectId}/attachments", UUID.randomUUID()).file(file))
                .andExpect(status().isBadRequest());
    }
}
