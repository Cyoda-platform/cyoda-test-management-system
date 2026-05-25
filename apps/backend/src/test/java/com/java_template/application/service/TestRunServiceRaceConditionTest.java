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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TestRunService — single-fetch on updateTestRun")
class TestRunServiceRaceConditionTest {

    @Mock private EntityService entityService;
    @Mock private ProjectCounterService projectCounterService;
    @Spy  private ObjectMapper objectMapper;

    @InjectMocks
    private TestRunService testRunService;

    private UUID runId;
    private UUID projectId;

    private EntityWithMetadata<TestRunDTO> wrap(TestRunDTO dto, UUID id) {
        EntityMetadata meta = new EntityMetadata();
        meta.setId(id);
        return new EntityWithMetadata<>(dto, meta);
    }

    @BeforeEach
    void setUp() {
        runId     = UUID.randomUUID();
        projectId = UUID.randomUUID();
    }

    @Test
    @DisplayName("fetches existing run exactly once even when caseIds and stepStatuses are missing")
    void fetchesExistingRunOnce_whenMergeNeeded() {
        // Existing run in 'active' state (not initial — no transition needed)
        TestRunDTO existing = new TestRunDTO();
        existing.setId(runId);
        existing.setProjectId(projectId);
        existing.setStatus("active");
        existing.setCaseIds("[\"case-1\"]");
        existing.setStepStatuses("{\"k\":\"PASSED\"}");

        // getTestRunById calls entityService.getById
        when(entityService.getById(eq(runId), any(), eq(TestRunDTO.class)))
                .thenReturn(wrap(existing, runId));

        // Incoming update with no caseIds and no stepStatuses — triggers merge
        TestRunDTO incoming = new TestRunDTO();
        incoming.setProjectId(projectId);
        incoming.setStatus("active");
        incoming.setName("Updated Name");
        incoming.setCaseIds(null);
        incoming.setStepStatuses(null);

        //noinspection unchecked
        doReturn(wrap(incoming, runId)).when(entityService).update(eq(runId), any(), isNull());

        testRunService.updateTestRun(runId, incoming);

        // entityService.getById must be called exactly once (not twice)
        verify(entityService, times(1)).getById(eq(runId), any(), eq(TestRunDTO.class));
    }

    @Test
    @DisplayName("fetches existing run exactly once when in initial state (transition check + merge)")
    void fetchesExistingRunOnce_whenInitialState() {
        TestRunDTO existing = new TestRunDTO();
        existing.setId(runId);
        existing.setProjectId(projectId);
        existing.setStatus("initial");
        existing.setCaseIds("[\"case-1\"]");
        existing.setStepStatuses("{\"k\":\"UNTESTED\"}");

        when(entityService.getById(eq(runId), any(), eq(TestRunDTO.class)))
                .thenReturn(wrap(existing, runId));

        TestRunDTO incoming = new TestRunDTO();
        incoming.setProjectId(projectId);
        incoming.setStatus("active");
        incoming.setName("Starting run");
        incoming.setCaseIds(null);      // triggers merge
        incoming.setStepStatuses(null); // triggers merge

        //noinspection unchecked
        doReturn(wrap(incoming, runId)).when(entityService).update(eq(runId), any(), eq("initialize_run"));

        testRunService.updateTestRun(runId, incoming);

        verify(entityService, times(1)).getById(eq(runId), any(), eq(TestRunDTO.class));
    }
}
