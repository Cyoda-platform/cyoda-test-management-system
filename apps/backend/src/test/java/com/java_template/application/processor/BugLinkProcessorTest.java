package com.java_template.application.processor;

import com.java_template.common.workflow.CyodaEventContext;
import com.java_template.common.workflow.OperationSpecification;
import io.cloudevents.v1.proto.CloudEvent;
import org.cyoda.cloud.api.event.common.ModelSpec;
import org.cyoda.cloud.api.event.processing.EntityProcessorCalculationRequest;
import org.cyoda.cloud.api.event.processing.EntityProcessorCalculationResponse;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DisplayName("BugLinkProcessor Tests")
class BugLinkProcessorTest {

    private BugLinkProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new BugLinkProcessor();
    }

    @Test
    @DisplayName("supports correct operation name")
    void supports_correctName_returnsTrue() {
        ModelSpec spec = new ModelSpec().withName("TestRunCase").withVersion(1);
        OperationSpecification.Processor opSpec = new OperationSpecification.Processor(
                spec, "BugLinkProcessor", "ACTIVE", "link_bug", "TestRunWorkflow");
        assertTrue(processor.supports(opSpec));
    }

    @Test
    @DisplayName("does not support a different operation name")
    void supports_wrongName_returnsFalse() {
        ModelSpec spec = new ModelSpec().withName("TestRunCase").withVersion(1);
        OperationSpecification.Processor opSpec = new OperationSpecification.Processor(
                spec, "SnapshotProcessor", "ACTIVE", "link_bug", "TestRunWorkflow");
        assertFalse(processor.supports(opSpec));
    }

    @Test
    @DisplayName("does not support empty operation name")
    void supports_emptyName_returnsFalse() {
        ModelSpec spec = new ModelSpec().withName("TestRunCase").withVersion(1);
        OperationSpecification.Processor opSpec = new OperationSpecification.Processor(
                spec, "", "ACTIVE", "link_bug", "TestRunWorkflow");
        assertFalse(processor.supports(opSpec));
    }

    @Test
    @DisplayName("process returns success=true")
    void process_validRequest_returnsSuccess() {
        EntityProcessorCalculationRequest request = createRequest();
        EntityProcessorCalculationResponse response = processor.process(createContext(request));
        assertNotNull(response);
        assertTrue(response.getSuccess());
    }

    @Test
    @DisplayName("process echoes the request id back in the response")
    void process_validRequest_preservesRequestId() {
        EntityProcessorCalculationRequest request = createRequest();
        request.setId("buglink-test-id");
        EntityProcessorCalculationResponse response = processor.process(createContext(request));
        assertEquals("buglink-test-id", response.getId());
    }

    private EntityProcessorCalculationRequest createRequest() {
        EntityProcessorCalculationRequest request = new EntityProcessorCalculationRequest();
        request.setId("req-001");
        request.setRequestId("r-456");
        request.setEntityId(UUID.randomUUID());
        return request;
    }

    private CyodaEventContext<EntityProcessorCalculationRequest> createContext(
            EntityProcessorCalculationRequest request) {
        return new CyodaEventContext<>() {
            @Override public CloudEvent getCloudEvent() { return mock(CloudEvent.class); }
            @Override public @NotNull EntityProcessorCalculationRequest getEvent() { return request; }
        };
    }
}
