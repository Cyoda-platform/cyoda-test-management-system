package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.Priority;
import com.java_template.application.dto.TestCaseDTO;
import com.java_template.application.dto.TestRunCaseDTO;
import com.java_template.application.dto.TestRunStepDTO;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.dto.PageResult;
import com.java_template.common.service.EntityService;
import com.java_template.common.workflow.CyodaEntity;
import org.cyoda.cloud.api.event.common.EntityMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TestRunCaseService")
class TestRunCaseServiceTest {

    @Mock private EntityService entityService;
    @Spy  private ObjectMapper objectMapper;
    @Mock private TestRunStepService testRunStepService;

    @InjectMocks
    private TestRunCaseService service;

    private UUID testRunId;
    private UUID testCaseId;
    private UUID projectId;
    private UUID runCaseId;

    private <T extends CyodaEntity> EntityWithMetadata<T> wrap(T dto, UUID id) {
        EntityMetadata meta = new EntityMetadata();
        meta.setId(id);
        return new EntityWithMetadata<>(dto, meta);
    }

    @BeforeEach
    void setUp() {
        testRunId  = UUID.randomUUID();
        testCaseId = UUID.randomUUID();
        projectId  = UUID.randomUUID();
        runCaseId  = UUID.randomUUID();
    }

    @Test
    @DisplayName("Creates TestRunCase normally when no existing case is found")
    void createsRunCaseWhenNoExisting() {
        TestRunCaseDTO input = new TestRunCaseDTO();
        input.setTestRunId(testRunId);
        input.setTestCaseId(testCaseId);
        input.setProjectId(projectId);
        input.setTitle("Login test");

        // Mock search to return empty list
        PageResult<EntityWithMetadata<TestRunCaseDTO>> emptyPage = PageResult.of(UUID.randomUUID(), List.of(), 1, 10, 0);
        when(entityService.search(any(), any(), eq(TestRunCaseDTO.class))).thenReturn(emptyPage);

        when(entityService.create(any(TestRunCaseDTO.class))).thenReturn(wrap(input, runCaseId));

        TestRunCaseDTO result = service.createTestRunCase(input);

        assertEquals(runCaseId, result.getId());
        verify(entityService).create(any(TestRunCaseDTO.class));
    }

    @Test
    @DisplayName("Returns existing TestRunCase when one already exists (Upsert logic)")
    void returnsExistingRunCase() {
        TestRunCaseDTO input = new TestRunCaseDTO();
        input.setTestRunId(testRunId);
        input.setTestCaseId(testCaseId);
        input.setProjectId(projectId);

        TestRunCaseDTO existing = new TestRunCaseDTO();
        existing.setId(runCaseId);

        PageResult<EntityWithMetadata<TestRunCaseDTO>> page = PageResult.of(UUID.randomUUID(), List.of(wrap(existing, runCaseId)), 1, 10, 1);
        when(entityService.search(any(), any(), eq(TestRunCaseDTO.class))).thenReturn(page);

        TestRunCaseDTO result = service.createTestRunCase(input);

        assertEquals(runCaseId, result.getId());
        verify(entityService, never()).create(any());
    }
}

