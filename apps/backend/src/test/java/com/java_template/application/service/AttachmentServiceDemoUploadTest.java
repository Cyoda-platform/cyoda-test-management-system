package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.java_template.application.dto.AttachmentDTO;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.service.EdgeMessageService;
import com.java_template.common.service.EntityService;
import org.cyoda.cloud.api.event.common.EntityMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceDemoUploadTest {

    @Mock private EntityService entityService;
    @Mock private EdgeMessageService edgeMessageService;

    private AttachmentService attachmentService;

    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID CASE_ID    = UUID.randomUUID();
    private static final UUID MESSAGE_ID = UUID.randomUUID();
    private static final String BASE64_CONTENT =
            Base64.getEncoder().encodeToString("fake-file-bytes".getBytes());

    @BeforeEach
    void setUp() {
        attachmentService = new AttachmentService(entityService, edgeMessageService, new ObjectMapper());
    }

    @Test
    void uploadFromBase64CreatesEdgeMessageWithDecodedBytes() throws Exception {
        when(edgeMessageService.createMessage(any(), any(ObjectNode.class), any()))
                .thenReturn(MESSAGE_ID);
        doReturn(entityWith("test.png")).when(entityService).create(any());

        attachmentService.uploadAttachmentFromBase64(
                PROJECT_ID, CASE_ID, null,
                "test.png", "image/png", BASE64_CONTENT,
                "CASE", null, null);

        ArgumentCaptor<ObjectNode> contentCaptor = ArgumentCaptor.forClass(ObjectNode.class);
        verify(edgeMessageService).createMessage(any(), contentCaptor.capture(), any());

        ObjectNode edgeContent = contentCaptor.getValue();
        assertThat(edgeContent.get("fileName").asText()).isEqualTo("test.png");
        assertThat(edgeContent.get("data").asText()).isEqualTo(BASE64_CONTENT);
    }

    @Test
    void uploadFromBase64SetsMessageIdOnEntity() throws Exception {
        when(edgeMessageService.createMessage(any(), any(ObjectNode.class), any()))
                .thenReturn(MESSAGE_ID);
        doReturn(entityWith("test.png")).when(entityService).create(any());

        attachmentService.uploadAttachmentFromBase64(
                PROJECT_ID, CASE_ID, null,
                "test.png", "image/png", BASE64_CONTENT,
                "CASE", null, null);

        ArgumentCaptor<AttachmentDTO> entityCaptor = ArgumentCaptor.forClass(AttachmentDTO.class);
        verify(entityService).create(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getMessageId()).isEqualTo(MESSAGE_ID);
    }

    @Test
    void uploadFromBase64SetsCorrectMetadata() throws Exception {
        when(edgeMessageService.createMessage(any(), any(ObjectNode.class), any()))
                .thenReturn(MESSAGE_ID);
        doReturn(entityWith("test.png")).when(entityService).create(any());

        attachmentService.uploadAttachmentFromBase64(
                PROJECT_ID, CASE_ID, null,
                "test.png", "image/png", BASE64_CONTENT,
                "CASE", null, null);

        ArgumentCaptor<AttachmentDTO> captor = ArgumentCaptor.forClass(AttachmentDTO.class);
        verify(entityService).create(captor.capture());
        AttachmentDTO dto = captor.getValue();

        assertThat(dto.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(dto.getCaseId()).isEqualTo(CASE_ID);
        assertThat(dto.getFileName()).isEqualTo("test.png");
        assertThat(dto.getFileType()).isEqualTo("image/png");
        assertThat(dto.getAttachmentType()).isEqualTo("CASE");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private EntityWithMetadata<AttachmentDTO> entityWith(String fileName) {
        AttachmentDTO dto = new AttachmentDTO();
        dto.setFileName(fileName);
        EntityMetadata meta = new EntityMetadata();
        meta.setId(UUID.randomUUID());
        return new EntityWithMetadata<>(dto, meta);
    }
}
