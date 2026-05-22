package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.TestRunDTO;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.dto.PageResult;
import com.java_template.common.service.EntityService;
import org.cyoda.cloud.api.event.common.EntityMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TestRunService
 */
@ExtendWith(MockitoExtension.class)
public class TestRunServiceTest {

    @Mock
    private EntityService entityService;

    @Mock
    private ProjectCounterService projectCounterService;

    @Spy
    private ObjectMapper objectMapper;

    @InjectMocks
    private TestRunService testRunService;

    private TestRunDTO testRun;
    private UUID runId;
    private UUID projectId;

    private EntityWithMetadata<TestRunDTO> entityWithMetadata(TestRunDTO dto, UUID id) {
        EntityMetadata metadata = new EntityMetadata();
        metadata.setId(id);
        return new EntityWithMetadata<>(dto, metadata);
    }

    @BeforeEach
    public void setUp() {
        runId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        testRun = new TestRunDTO();
        testRun.setProjectId(projectId);
        testRun.setName("Test Run 1");
        testRun.setEnvironment("STAGING");
    }

    @Test
    public void testCreateTestRun() {
        when(projectCounterService.nextRunDisplayId(projectId)).thenReturn("RUN-001");
        when(entityService.create(any(TestRunDTO.class)))
                .thenAnswer(inv -> entityWithMetadata(inv.getArgument(0), runId));
        // createTestRun calls entityService.update after create (to persist displayId + set status)
        when(entityService.update(eq(runId), any(TestRunDTO.class), isNull()))
                .thenAnswer(inv -> entityWithMetadata(inv.getArgument(1), runId));

        TestRunDTO created = testRunService.createTestRun(testRun);

        assertNotNull(created.getId());
        assertEquals("Test Run 1", created.getName());
        assertNotNull(created.getStartedAt());
    }

    @Test
    public void testGetTestRunById() {
        testRun.setId(runId);
        when(entityService.getById(eq(runId), any(), eq(TestRunDTO.class)))
                .thenReturn(entityWithMetadata(testRun, runId));

        Optional<TestRunDTO> retrieved = testRunService.getTestRunById(runId);

        assertTrue(retrieved.isPresent());
        assertEquals(runId, retrieved.get().getId());
    }

    @Test
    public void testGetTestRunsByProjectId() {
        PageResult<EntityWithMetadata<TestRunDTO>> page =
                PageResult.of(null, List.of(entityWithMetadata(testRun, runId)), 0, 20, 1);
        when(entityService.findAll(any(), eq(TestRunDTO.class))).thenReturn(page);

        var result = testRunService.getTestRunsByProjectId(projectId, 0, 20);

        assertFalse(result.data().isEmpty());
        assertEquals(1, result.data().size());
    }

    @Test
    public void testDeleteTestRun() {
        when(entityService.getById(eq(runId), any(), eq(TestRunDTO.class)))
                .thenReturn(entityWithMetadata(testRun, runId));
        when(entityService.deleteById(runId)).thenReturn(runId);

        boolean deleted = testRunService.deleteTestRun(runId);

        assertTrue(deleted);
        verify(entityService).deleteById(runId);
    }

    @Test
    void updateTestRun_notFound_returnsEmpty() {
        when(entityService.getById(eq(runId), any(), eq(TestRunDTO.class)))
                .thenThrow(new RuntimeException("Entity not found"));

        Optional<TestRunDTO> result = testRunService.updateTestRun(runId, testRun);

        assertFalse(result.isPresent());
        verify(entityService, never()).update(any(), any(), any());
    }

    @Test
    void deleteTestRun_notFound_returnsFalse() {
        when(entityService.getById(eq(runId), any(), eq(TestRunDTO.class)))
                .thenThrow(new RuntimeException("Entity not found"));

        boolean deleted = testRunService.deleteTestRun(runId);

        assertFalse(deleted);
        verify(entityService, never()).deleteById(any());
    }

    @Test
    void createTestRun_with150PlusCaseIds_storesViaCaseIdsJsonNotArray() {
        List<String> caseIds = IntStream.range(0, 155)
                .mapToObj(i -> UUID.randomUUID().toString())
                .collect(Collectors.toList());
        testRun.setCaseIds(caseIds);

        when(projectCounterService.nextRunDisplayId(projectId)).thenReturn("TR-01");
        when(entityService.create(any())).thenAnswer(inv -> entityWithMetadata(inv.getArgument(0), runId));

        // Capture the DTO state AT THE MOMENT EntityService.update is called —
        // ArgumentCaptor only holds a reference, so we must snapshot mutable fields immediately.
        final String[] capturedCaseIdsJson = {null};
        final boolean[] capturedCaseIdsNull = {false};
        when(entityService.update(eq(runId), any(TestRunDTO.class), isNull()))
                .thenAnswer(inv -> {
                    TestRunDTO payload = inv.getArgument(1);
                    capturedCaseIdsJson[0] = payload.getCaseIdsJson();
                    capturedCaseIdsNull[0] = (payload.getCaseIds() == null || payload.getCaseIds().isEmpty());
                    return entityWithMetadata(payload, runId);
                });

        TestRunDTO created = testRunService.createTestRun(testRun);

        // Cyoda must NOT receive caseIds as an array (would create 155 schema fields → limit exceeded)
        assertTrue(capturedCaseIdsNull[0], "caseIds array must be null in the Cyoda payload");
        // Must be stored as a single JSON string field instead
        assertNotNull(capturedCaseIdsJson[0], "caseIdsJson must be populated in the Cyoda payload");

        // HTTP response must still expose caseIds as a list
        assertNotNull(created.getCaseIds());
        assertEquals(155, created.getCaseIds().size());
        assertNull(created.getCaseIdsJson(), "caseIdsJson must not appear in the HTTP response");
    }

    @Test
    void getTestRunById_restoresCaseIdsFromCaseIdsJson() {
        List<String> expected = List.of("id-a", "id-b", "id-c");
        TestRunDTO stored = new TestRunDTO();
        stored.setId(runId);
        stored.setName("Run");
        stored.setCaseIds(null);
        stored.setCaseIdsJson("[\"id-a\",\"id-b\",\"id-c\"]");

        when(entityService.getById(eq(runId), any(), eq(TestRunDTO.class)))
                .thenReturn(entityWithMetadata(stored, runId));

        Optional<TestRunDTO> result = testRunService.getTestRunById(runId);

        assertTrue(result.isPresent());
        assertEquals(expected, result.get().getCaseIds(), "caseIds must be restored from caseIdsJson");
        assertNull(result.get().getCaseIdsJson(), "caseIdsJson must not be exposed in HTTP response");
    }
}

