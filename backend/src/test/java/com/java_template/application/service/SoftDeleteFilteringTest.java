package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.SuiteDTO;
import com.java_template.application.dto.TestCaseDTO;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Tests for Soft Delete Filtering - FR 2.8
 * Verifies that deleted items are properly filtered from results
 */
@ExtendWith(MockitoExtension.class)
public class SoftDeleteFilteringTest {

    @Mock
    private EntityService entityService;

    @Spy
    private ObjectMapper objectMapper;

    @InjectMocks
    private SuiteService suiteService;

    @InjectMocks
    private TestCaseService testCaseService;

    private UUID projectId;
    private UUID suiteId;
    private UUID caseId;

    private EntityWithMetadata<SuiteDTO> entityWithMetadata(SuiteDTO dto, UUID id) {
        EntityMetadata metadata = new EntityMetadata();
        metadata.setId(id);
        return new EntityWithMetadata<>(dto, metadata);
    }

    private EntityWithMetadata<TestCaseDTO> entityWithMetadata(TestCaseDTO dto, UUID id) {
        EntityMetadata metadata = new EntityMetadata();
        metadata.setId(id);
        return new EntityWithMetadata<>(dto, metadata);
    }

    @BeforeEach
    public void setUp() {
        projectId = UUID.randomUUID();
        suiteId = UUID.randomUUID();
        caseId = UUID.randomUUID();
    }

    @Test
    @DisplayName("getTestCaseById filters out deleted cases")
    public void testGetTestCaseByIdFiltersDeleted() {
        TestCaseDTO deletedCase = new TestCaseDTO();
        deletedCase.setId(caseId);
        deletedCase.setDeleted(true);

        when(entityService.getById(eq(caseId), any(), eq(TestCaseDTO.class)))
                .thenReturn(entityWithMetadata(deletedCase, caseId));

        Optional<TestCaseDTO> result = testCaseService.getTestCaseById(caseId);

        assertTrue(result.isEmpty(), "Deleted test case should be filtered out");
    }

    @Test
    @DisplayName("getTestCaseById returns active cases")
    public void testGetTestCaseByIdReturnsActive() {
        TestCaseDTO activeCase = new TestCaseDTO();
        activeCase.setId(caseId);
        activeCase.setDeleted(false);

        when(entityService.getById(eq(caseId), any(), eq(TestCaseDTO.class)))
                .thenReturn(entityWithMetadata(activeCase, caseId));

        Optional<TestCaseDTO> result = testCaseService.getTestCaseById(caseId);

        assertTrue(result.isPresent(), "Active test case should be returned");
        assertFalse(result.get().isDeleted());
    }

}
