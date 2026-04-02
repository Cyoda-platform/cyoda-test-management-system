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
    void testCreateTestStepSetsDefaultStatus() {
        when(entityService.create(any(TestStepDTO.class)))
                .thenAnswer(inv -> entityWithMetadata(inv.getArgument(0), stepId));

        TestStepDTO created = testStepService.createTestStep(testStep);

        assertNotNull(created.getId());
        assertEquals("untested", created.getStatus());
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
    void testGetTestStepsByTestCaseIdFiltersAndSorts() {
        UUID otherStepId = UUID.randomUUID();
        UUID otherCaseId = UUID.randomUUID();

        TestStepDTO third = new TestStepDTO();
        third.setTestCaseId(caseId);
        third.setStepNumber(3);
        third.setAction("Step 3");

        TestStepDTO first = new TestStepDTO();
        first.setTestCaseId(caseId);
        first.setStepNumber(1);
        first.setAction("Step 1");

        TestStepDTO other = new TestStepDTO();
        other.setTestCaseId(otherCaseId);
        other.setStepNumber(2);
        other.setAction("Other case step");

        when(entityService.findAll(any(), eq(TestStepDTO.class), any()))
                .thenReturn(PageResult.of(
                        null,
                        List.of(
                                entityWithMetadata(third, stepId),
                                entityWithMetadata(first, UUID.randomUUID()),
                                entityWithMetadata(other, otherStepId)
                        ),
                        0,
                        1000,
                        3
                ));

        List<TestStepDTO> result = testStepService.getTestStepsByTestCaseId(caseId);

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getStepNumber());
        assertEquals(3, result.get(1).getStepNumber());
        assertTrue(result.stream().allMatch(step -> caseId.equals(step.getTestCaseId())));
    }

    @Test
    void testDeleteTestStep() {
        when(entityService.deleteById(stepId)).thenReturn(stepId);

        boolean deleted = testStepService.deleteTestStep(stepId);

        assertTrue(deleted);
        verify(entityService).deleteById(stepId);
    }
}