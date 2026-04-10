package com.java_template.application.service;

import com.java_template.application.dto.TestStepDTO;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.dto.PageResult;
import com.java_template.common.service.EntityService;
import org.cyoda.cloud.api.event.common.EntityMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestStepServiceTest {

    @Mock
    private EntityService entityService;

    @InjectMocks
    private TestStepService testStepService;

    private TestStepDTO testStep;
    private UUID stepId;
    private UUID caseId;

    private EntityWithMetadata<TestStepDTO> entityWithMetadata(TestStepDTO dto, UUID id) {
        EntityMetadata metadata = new EntityMetadata();
        metadata.setId(id);
        return new EntityWithMetadata<>(dto, metadata);
    }

    @BeforeEach
    void setUp() {
        stepId = UUID.randomUUID();
        caseId = UUID.randomUUID();
        testStep = new TestStepDTO();
        testStep.setTestCaseId(caseId);
        testStep.setStepNumber(2);
        testStep.setAction("Open login page");
        testStep.setExpectedResult("Login page is visible");
    }

    @Test
    void testCreateTestStep() {
        when(entityService.create(any(TestStepDTO.class)))
                .thenAnswer(inv -> entityWithMetadata(inv.getArgument(0), stepId));

        TestStepDTO created = testStepService.createTestStep(testStep);

        assertNotNull(created.getId());
        assertEquals(caseId, created.getTestCaseId());
    }

    @Test
    void testGetTestStepById() {
        when(entityService.getById(eq(stepId), any(), eq(TestStepDTO.class)))
                .thenReturn(entityWithMetadata(testStep, stepId));

        Optional<TestStepDTO> retrieved = testStepService.getTestStepById(stepId);

        assertTrue(retrieved.isPresent());
        assertEquals(stepId, retrieved.get().getId());
    }

    @Test
    void testDeleteTestStep() {
        when(entityService.deleteById(stepId)).thenReturn(stepId);

        boolean deleted = testStepService.deleteTestStep(stepId);

        assertTrue(deleted);
        verify(entityService).deleteById(stepId);
    }

    @Test
    void testUpdateTestStep() {
        testStep.setId(stepId);
        TestStepDTO updated = new TestStepDTO();
        updated.setAction("Updated action");
        updated.setExpectedResult("Updated result");
        updated.setStepNumber(2);

        when(entityService.update(eq(stepId), any(TestStepDTO.class), isNull()))
                .thenReturn(entityWithMetadata(updated, stepId));

        TestStepDTO result = testStepService.updateTestStep(stepId, updated);

        assertEquals("Updated action", result.getAction());
        assertEquals("Updated result", result.getExpectedResult());
        assertEquals(2, result.getStepNumber());
        verify(entityService).update(eq(stepId), any(TestStepDTO.class), isNull());
    }
}