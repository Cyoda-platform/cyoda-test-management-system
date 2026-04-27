package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.SuiteDTO;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.dto.PageResult;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SuiteService - FR 2.1, 2.13
 */
@ExtendWith(MockitoExtension.class)
public class SuiteServiceTest {

    @Mock
    private EntityService entityService;

    @Spy
    private ObjectMapper objectMapper;

    @Mock
    private TestCaseService testCaseService;

    @InjectMocks
    private SuiteService suiteService;

    private SuiteDTO suite;
    private UUID suiteId;
    private UUID projectId;

    private EntityWithMetadata<SuiteDTO> entityWithMetadata(SuiteDTO dto, UUID id) {
        EntityMetadata metadata = new EntityMetadata();
        metadata.setId(id);
        return new EntityWithMetadata<>(dto, metadata);
    }

    @BeforeEach
    public void setUp() {
        suiteId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        suite = new SuiteDTO();
        suite.setProjectId(projectId);
        suite.setName("Test Suite");
        suite.setDescription("A test suite");
    }

    @Test
    @DisplayName("Should create suite successfully")
    public void testCreateSuite() {
        when(entityService.create(any(SuiteDTO.class)))
                .thenAnswer(inv -> entityWithMetadata(inv.getArgument(0), suiteId));
        when(entityService.search(any(), any(), eq(SuiteDTO.class), any()))
                .thenReturn(PageResult.of(null, List.of(), 0, 1000, 0));

        SuiteDTO created = suiteService.createSuite(suite);

        assertNotNull(created.getId());
        assertEquals("Test Suite", created.getName());
        assertEquals(projectId, created.getProjectId());
        verify(entityService).create(any(SuiteDTO.class));
    }

    @Test
    @DisplayName("Should get suite by ID")
    public void testGetSuiteById() {
        suite.setId(suiteId);
        when(entityService.getById(eq(suiteId), any(), eq(SuiteDTO.class)))
                .thenReturn(entityWithMetadata(suite, suiteId));

        Optional<SuiteDTO> retrieved = suiteService.getSuiteById(suiteId);

        assertTrue(retrieved.isPresent());
        assertEquals(suiteId, retrieved.get().getId());
        assertEquals("Test Suite", retrieved.get().getName());
    }

    @Test
    @DisplayName("Should get suites by project ID")
    public void testGetSuitesByProjectId() {
        suite.setId(suiteId);
        PageResult<EntityWithMetadata<SuiteDTO>> page =
                PageResult.of(null, List.of(entityWithMetadata(suite, suiteId)), 0, 20, 1);
        when(entityService.search(any(), any(), eq(SuiteDTO.class), any())).thenReturn(page);

        var result = suiteService.getSuitesByProjectId(projectId, 0, 20);

        assertFalse(result.data().isEmpty());
        assertEquals(1, result.data().size());
    }

    @Test
    @DisplayName("Should update suite")
    public void testUpdateSuite() {
        suite.setId(suiteId);
        SuiteDTO updated = new SuiteDTO();
        updated.setName("Updated Suite");
        updated.setDescription("Updated description");

        when(entityService.update(eq(suiteId), any(SuiteDTO.class), isNull()))
                .thenReturn(entityWithMetadata(updated, suiteId));

        SuiteDTO result = suiteService.updateSuite(suiteId, updated);

        assertEquals("Updated Suite", result.getName());
        assertEquals("Updated description", result.getDescription());
    }

    @Test
    @DisplayName("Should delete suite")
    public void testDeleteSuite() {
        // deleteSuite first soft-deletes all test cases, then hard-deletes the suite itself
        when(testCaseService.getAllTestCasesBySuiteId(suiteId))
                .thenReturn(java.util.List.of());
        when(entityService.deleteById(suiteId)).thenReturn(suiteId);

        boolean deleted = suiteService.deleteSuite(suiteId);

        assertTrue(deleted);
        verify(entityService).deleteById(suiteId);
    }

    @Test
    @DisplayName("Should return true when suite exists")
    public void testSuiteExists() {
        suite.setId(suiteId);
        when(entityService.getById(eq(suiteId), any(), eq(SuiteDTO.class)))
                .thenReturn(entityWithMetadata(suite, suiteId));

        boolean exists = suiteService.suiteExists(suiteId);

        assertTrue(exists);
    }

    @Test
    @DisplayName("Should return false when suite does not exist")
    public void testSuiteNotExists() {
        when(entityService.getById(eq(suiteId), any(), eq(SuiteDTO.class)))
                .thenThrow(new RuntimeException("Not found"));

        boolean exists = suiteService.suiteExists(suiteId);

        assertFalse(exists);
    }

}
