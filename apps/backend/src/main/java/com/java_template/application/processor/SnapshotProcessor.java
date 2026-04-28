package com.java_template.application.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.TestCaseDTO;
import com.java_template.application.dto.TestRunCaseDTO;
import com.java_template.application.service.TestCaseService;
import com.java_template.application.service.TestRunCaseService;
import com.java_template.common.workflow.CyodaEventContext;
import com.java_template.common.workflow.CyodaProcessor;
import com.java_template.common.workflow.OperationSpecification;
import org.cyoda.cloud.api.event.processing.EntityProcessorCalculationRequest;
import org.cyoda.cloud.api.event.processing.EntityProcessorCalculationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Processor for capturing an immutable snapshot of all TestCase and TestStep data
 * at the moment a TestRun transitions from {@code initial} to {@code active}.
 *
 * <p>For each case ID listed in the TestRun payload, it fetches the current
 * {@link TestCaseDTO} and its {@link TestStepDTO} records, then creates
 * {@link TestRunCaseDTO} and {@link TestRunStepDTO} entities that embed the
 * full content. This means the run remains self-contained: even if the original
 * TestCase or TestStep is soft-deleted later, the historical run data is preserved.</p>
 *
 * <p>Constraint: this processor must NOT call {@code EntityService} to modify the
 * TestRun entity being processed (Cyoda would deadlock). Creating new TestRunCase /
 * TestRunStep entities is safe because they are separate entity types.</p>
 */
@Component("SnapshotProcessor")
public class SnapshotProcessor implements CyodaProcessor {

    private static final Logger log = LoggerFactory.getLogger(SnapshotProcessor.class);
    private static final String PROCESSOR_NAME = SnapshotProcessor.class.getSimpleName();

    private final TestCaseService testCaseService;
    private final TestRunCaseService testRunCaseService;
    private final ObjectMapper objectMapper;

    public SnapshotProcessor(TestCaseService testCaseService,
                              TestRunCaseService testRunCaseService,
                              ObjectMapper objectMapper) {
        this.testCaseService    = testCaseService;
        this.testRunCaseService = testRunCaseService;
        this.objectMapper       = objectMapper;
    }

    @Override
    public EntityProcessorCalculationResponse process(
            CyodaEventContext<EntityProcessorCalculationRequest> context) {

        EntityProcessorCalculationRequest request = context.getEvent();
        UUID testRunId = request.getEntityId();
        log.info("SnapshotProcessor starting for testRunId={}", testRunId);

        try {
            JsonNode data = request.getPayload().getData();

            UUID projectId = parseUuid(data, "projectId", testRunId);
            List<String> caseIds = parseCaseIds(data);

            if (caseIds.isEmpty()) {
                log.info("SnapshotProcessor: no caseIds in TestRun {}, nothing to snapshot", testRunId);
                return successResponse(request);
            }

            for (String caseIdStr : caseIds) {
                snapshotCase(testRunId, projectId, caseIdStr);
            }

            log.info("SnapshotProcessor completed for testRunId={}, snapshotted {} case(s)",
                    testRunId, caseIds.size());

        } catch (Exception e) {
            log.error("SnapshotProcessor failed for testRunId={}: {}", testRunId, e.getMessage(), e);
            // Return success anyway — the run must not be blocked by a snapshot failure.
        }

        return successResponse(request);
    }

    private void snapshotCase(UUID testRunId, UUID projectId, String caseIdStr) {
        UUID caseId;
        try {
            caseId = UUID.fromString(caseIdStr);
        } catch (IllegalArgumentException e) {
            log.warn("SnapshotProcessor: invalid caseId '{}', skipping", caseIdStr);
            return;
        }

        Optional<TestCaseDTO> tcOpt = testCaseService.getTestCaseById(caseId);
        if (tcOpt.isEmpty()) {
            log.warn("SnapshotProcessor: TestCase {} not found (already deleted?), skipping", caseId);
            return;
        }
        TestCaseDTO tc = tcOpt.get();

        TestRunCaseDTO caseSnapshot = new TestRunCaseDTO();
        caseSnapshot.setTestRunId(testRunId);
        caseSnapshot.setTestCaseId(caseId);
        caseSnapshot.setProjectId(projectId);
        caseSnapshot.setTitle(tc.getTitle());
        caseSnapshot.setDescription(tc.getDescription());
        caseSnapshot.setPreconditions(tc.getPreconditions());
        caseSnapshot.setPriority(tc.getPriority());
        caseSnapshot.setDisplayId(tc.getDisplayId());
        caseSnapshot.setSuiteId(tc.getSuiteId());

        List<TestCaseDTO.StepDTO> steps = tc.getSteps();

        testRunCaseService.createTestRunCaseWithStepSnapshots(caseSnapshot, steps);
        log.debug("SnapshotProcessor: snapshotted case {} with {} step(s)", caseId, steps.size());
    }

    private UUID parseUuid(JsonNode data, String field, UUID testRunId) {
        JsonNode node = data.get(field);
        if (node == null || node.isNull()) return null;
        try {
            return UUID.fromString(node.asText());
        } catch (IllegalArgumentException e) {
            log.warn("SnapshotProcessor: cannot parse UUID field '{}' for testRunId={}: {}",
                    field, testRunId, node.asText());
            return null;
        }
    }

    private List<String> parseCaseIds(JsonNode data) {
        JsonNode node = data.get("caseIds");
        if (node == null || !node.isArray() || node.isEmpty()) return List.of();
        return objectMapper.convertValue(node,
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
    }

    private EntityProcessorCalculationResponse successResponse(EntityProcessorCalculationRequest request) {
        EntityProcessorCalculationResponse response = new EntityProcessorCalculationResponse();
        response.setId(request.getId());
        response.setSuccess(true);
        return response;
    }

    @Override
    public boolean supports(OperationSpecification opSpec) {
        return PROCESSOR_NAME.equals(opSpec.operationName());
    }
}
