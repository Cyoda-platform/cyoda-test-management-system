package com.java_template.application.criterion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.java_template.common.serializer.CriterionSerializer;
import com.java_template.common.serializer.SerializerFactory;
import com.java_template.common.serializer.jackson.JacksonCriterionSerializer;
import com.java_template.common.workflow.CyodaEventContext;
import com.java_template.common.workflow.OperationSpecification;
import io.cloudevents.v1.proto.CloudEvent;
import org.cyoda.cloud.api.event.common.DataPayload;
import org.cyoda.cloud.api.event.common.ModelSpec;
import org.cyoda.cloud.api.event.processing.EntityCriteriaCalculationRequest;
import org.cyoda.cloud.api.event.processing.EntityCriteriaCalculationResponse;
import org.cyoda.uuid.SimpleSystemClock;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class RequireAdminRoleCriterionTest {

    private static final UUID ENTITY_ID = SimpleSystemClock.INSTANCE.uniqueTimeUUIDinMicros();

    private final JacksonCriterionSerializer criterionSerializer = new JacksonCriterionSerializer(new ObjectMapper());
    private final SerializerFactory serializerFactory = new SerializerFactory(List.of(), List.of((CriterionSerializer) criterionSerializer));
    private final RequireAdminRoleCriterion criterion = new RequireAdminRoleCriterion(serializerFactory);

    @Test
    void supports_matchingOperationName_returnsTrue() {
        ModelSpec modelSpec = new ModelSpec();
        modelSpec.setName("Project");
        modelSpec.setVersion(1);

        OperationSpecification.Criterion opsSpec = new OperationSpecification.Criterion(
                modelSpec, "RequireAdminRole", "initial", "create_project", "Project");

        assertTrue(criterion.supports(opsSpec));
    }

    @Test
    void supports_differentOperationName_returnsFalse() {
        ModelSpec modelSpec = new ModelSpec();
        modelSpec.setName("Project");
        modelSpec.setVersion(1);

        OperationSpecification.Criterion opsSpec = new OperationSpecification.Criterion(
                modelSpec, "RequireTesterOrAdminRole", "initial", "create_report", "Report");

        assertFalse(criterion.supports(opsSpec));
    }

    @Test
    void check_returnsMatchesTrue() {
        CyodaEventContext<EntityCriteriaCalculationRequest> context = buildContext();

        EntityCriteriaCalculationResponse response = criterion.check(context);

        assertTrue(response.getMatches());
        assertTrue(response.getSuccess());
        assertEquals(ENTITY_ID, response.getEntityId());
    }

    @NotNull
    private CyodaEventContext<EntityCriteriaCalculationRequest> buildContext() {
        EntityCriteriaCalculationRequest request = new EntityCriteriaCalculationRequest();
        request.setId("req-1");
        request.setRequestId("req-1");
        request.setEntityId(ENTITY_ID);

        DataPayload payload = new DataPayload();
        ObjectNode data = new ObjectMapper().createObjectNode();
        data.put("name", "test-project");
        payload.setData(data);
        request.setPayload(payload);

        return new CyodaEventContext<>() {
            @Override
            public CloudEvent getCloudEvent() {
                return mock(CloudEvent.class);
            }

            @Override
            public @NotNull EntityCriteriaCalculationRequest getEvent() {
                return request;
            }
        };
    }
}
