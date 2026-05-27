package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.TestRunDTO;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.service.EntityService;
import org.cyoda.cloud.api.event.common.EntityMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TestRunService — createTestRun")
class TestRunServiceCreateTest {

    @Mock private EntityService entityService;
    @Mock private ProjectCounterService projectCounterService;
    @Spy  private ObjectMapper objectMapper;

    @InjectMocks
    private TestRunService testRunService;

    private UUID projectId;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        when(projectCounterService.nextRunDisplayId(any())).thenReturn("TR-1");
    }

    private EntityWithMetadata<TestRunDTO> wrap(TestRunDTO dto, UUID id) {
        EntityMetadata meta = new EntityMetadata();
        meta.setId(id);
        return new EntityWithMetadata<>(dto, meta);
    }

    @Test
    @DisplayName("deletes orphan entity when follow-up update fails")
    void createTestRun_deletesOrphanEntityWhenUpdateFails() {
        UUID runId = UUID.randomUUID();
        TestRunDTO dto = new TestRunDTO();
        dto.setProjectId(projectId);
        dto.setName("My Run");
        dto.setCaseIds("[\"" + UUID.randomUUID() + "\"]");

        //noinspection unchecked
        doReturn(wrap(dto, runId)).when(entityService).create(any());
        doThrow(new RuntimeException("Cyoda timeout on update"))
                .when(entityService).update(eq(runId), any(), isNull());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> testRunService.createTestRun(dto));

        assertTrue(ex.getMessage().contains("Cyoda timeout on update"));
        // Compensation: orphan entity must be deleted so the client can safely retry
        verify(entityService).deleteById(runId);
    }

    @Test
    @DisplayName("returns created run when both create and update succeed")
    void createTestRun_returnsRunWhenBothCallsSucceed() {
        UUID runId = UUID.randomUUID();
        TestRunDTO dto = new TestRunDTO();
        dto.setProjectId(projectId);
        dto.setName("Happy path run");
        dto.setCaseIds("[\"" + UUID.randomUUID() + "\"]");

        TestRunDTO created = new TestRunDTO();
        created.setProjectId(projectId);
        created.setName("Happy path run");

        //noinspection unchecked
        doReturn(wrap(created, runId)).when(entityService).create(any());
        //noinspection unchecked
        doReturn(wrap(created, runId)).when(entityService).update(eq(runId), any(), isNull());

        TestRunDTO result = testRunService.createTestRun(dto);

        assertNotNull(result);
        assertEquals(runId, result.getId());
        verify(entityService, never()).deleteById(any());
    }
}
