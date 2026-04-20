package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.AttachmentDTO;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.dto.PageResult;
import com.java_template.common.service.EdgeMessageService;
import com.java_template.common.service.EntityService;
import org.cyoda.cloud.api.common.model.GroupConditionDto;
import org.cyoda.cloud.api.event.common.EntityMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttachmentService unit tests")
class AttachmentServiceTest {

    @Mock private EntityService entityService;
    @Mock private EdgeMessageService edgeMessageService;
    @Spy  private ObjectMapper objectMapper;

    @InjectMocks
    private AttachmentService attachmentService;

    private UUID projectId;
    private UUID caseId;
    private UUID defectId;
    private UUID runId;

    private EntityWithMetadata<AttachmentDTO> wrap(AttachmentDTO dto, UUID id) {
        EntityMetadata meta = new EntityMetadata();
        meta.setId(id);
        return new EntityWithMetadata<>(dto, meta);
    }

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        caseId    = UUID.randomUUID();
        defectId  = UUID.randomUUID();
        runId     = UUID.randomUUID();
    }

    @Test
    @DisplayName("uploadAttachment sets attachmentType=CASE when not provided")
    void uploadFile_defaultsToCase() throws Exception {
        UUID attId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "shot.png", "image/png", new byte[]{1, 2, 3});
        when(edgeMessageService.createMessage(any(), any(), any())).thenReturn(UUID.randomUUID());
        when(entityService.create(any())).thenAnswer(inv -> wrap(inv.getArgument(0), attId));

        AttachmentDTO result = attachmentService.uploadAttachment(
                projectId, caseId, null, file, null, null, null);

        assertEquals("CASE", result.getAttachmentType());
        assertNull(result.getRunId());
        assertNull(result.getStepKey());
    }

    @Test
    @DisplayName("uploadAttachment sets EVIDENCE type with runId and stepKey")
    void uploadFile_setsEvidenceFields() throws Exception {
        UUID attId  = UUID.randomUUID();
        String stepKey = "2";
        MockMultipartFile file = new MockMultipartFile("file", "shot.png", "image/png", new byte[]{1, 2, 3});
        when(edgeMessageService.createMessage(any(), any(), any())).thenReturn(UUID.randomUUID());
        when(entityService.create(any())).thenAnswer(inv -> wrap(inv.getArgument(0), attId));

        AttachmentDTO result = attachmentService.uploadAttachment(
                projectId, caseId, null, file, "EVIDENCE", runId, stepKey);

        assertEquals("EVIDENCE", result.getAttachmentType());
        assertEquals(runId, result.getRunId());
        assertEquals(stepKey, result.getStepKey());
    }

    @Test
    @DisplayName("uploadAttachment sets DEFECT type with defectId")
    void uploadFile_setsDefectFields() throws Exception {
        UUID attId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "log.txt", "text/plain", new byte[]{1});
        when(edgeMessageService.createMessage(any(), any(), any())).thenReturn(UUID.randomUUID());
        when(entityService.create(any())).thenAnswer(inv -> wrap(inv.getArgument(0), attId));

        AttachmentDTO result = attachmentService.uploadAttachment(
                projectId, null, defectId, file, "DEFECT", null, null);

        assertEquals("DEFECT", result.getAttachmentType());
        assertEquals(defectId, result.getDefectId());
    }

    @Test
    @DisplayName("getEvidenceByRunCaseStep queries with 4-field compound condition")
    void getEvidenceByRunCaseStep_queriesCorrectly() {
        UUID attId  = UUID.randomUUID();
        String stepKey = "3";

        AttachmentDTO evidence = new AttachmentDTO();
        evidence.setAttachmentType("EVIDENCE");
        evidence.setRunId(runId);
        evidence.setCaseId(caseId);
        evidence.setStepKey(stepKey);

        PageResult<EntityWithMetadata<AttachmentDTO>> page =
                PageResult.of(null, List.of(wrap(evidence, attId)), 0, 20, 1L);

        ArgumentCaptor<GroupConditionDto> conditionCaptor = ArgumentCaptor.forClass(GroupConditionDto.class);
        when(entityService.search(any(), conditionCaptor.capture(), eq(AttachmentDTO.class)))
                .thenReturn(page);

        List<AttachmentDTO> result = attachmentService.getEvidenceByRunCaseStep(runId, caseId, stepKey);

        assertEquals(1, result.size());
        assertEquals("EVIDENCE", result.get(0).getAttachmentType());
        assertEquals(stepKey, result.get(0).getStepKey());

        // Verify the compound condition has exactly 4 leaf conditions
        GroupConditionDto capturedGroup = conditionCaptor.getValue();
        assertNotNull(capturedGroup);
        assertEquals(4, capturedGroup.getConditions().size(), "Expected 4 conditions in AND group");

        // Verify the JSON paths covered — attachmentType, runId, caseId, stepKey
        List<String> paths = capturedGroup.getConditions().stream()
                .map(c -> ((org.cyoda.cloud.api.common.model.SimpleConditionDto) c).getJsonPath())
                .toList();
        assertTrue(paths.contains("$.attachmentType"), "Missing $.attachmentType condition");
        assertTrue(paths.contains("$.runId"),           "Missing $.runId condition");
        assertTrue(paths.contains("$.caseId"),          "Missing $.caseId condition");
        assertTrue(paths.contains("$.stepKey"),         "Missing $.stepKey condition");
    }
}
