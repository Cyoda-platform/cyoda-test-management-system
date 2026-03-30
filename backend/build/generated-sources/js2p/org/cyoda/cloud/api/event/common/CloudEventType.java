
package org.cyoda.cloud.api.event.common;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;


/**
 * CloudEventType
 * <p>
 * 
 * 
 */
@Generated("jsonschema2pojo")
public enum CloudEventType {

    BASE_EVENT("BaseEvent"),
    CALCULATION_MEMBER_JOIN_EVENT("CalculationMemberJoinEvent"),
    CALCULATION_MEMBER_GREET_EVENT("CalculationMemberGreetEvent"),
    ENTITY_PROCESSOR_CALCULATION_REQUEST("EntityProcessorCalculationRequest"),
    ENTITY_PROCESSOR_CALCULATION_RESPONSE("EntityProcessorCalculationResponse"),
    EVENT_ACK_RESPONSE("EventAckResponse"),
    ENTITY_CRITERIA_CALCULATION_REQUEST("EntityCriteriaCalculationRequest"),
    ENTITY_CRITERIA_CALCULATION_RESPONSE("EntityCriteriaCalculationResponse"),
    CALCULATION_MEMBER_KEEP_ALIVE_EVENT("CalculationMemberKeepAliveEvent"),
    ENTITY_MODEL_IMPORT_REQUEST("EntityModelImportRequest"),
    ENTITY_MODEL_IMPORT_RESPONSE("EntityModelImportResponse"),
    ENTITY_MODEL_EXPORT_REQUEST("EntityModelExportRequest"),
    ENTITY_MODEL_EXPORT_RESPONSE("EntityModelExportResponse"),
    ENTITY_MODEL_TRANSITION_REQUEST("EntityModelTransitionRequest"),
    ENTITY_MODEL_TRANSITION_RESPONSE("EntityModelTransitionResponse"),
    ENTITY_MODEL_DELETE_REQUEST("EntityModelDeleteRequest"),
    ENTITY_MODEL_DELETE_RESPONSE("EntityModelDeleteResponse"),
    ENTITY_MODEL_GET_ALL_REQUEST("EntityModelGetAllRequest"),
    ENTITY_MODEL_GET_ALL_RESPONSE("EntityModelGetAllResponse"),
    ENTITY_CREATE_REQUEST("EntityCreateRequest"),
    ENTITY_CREATE_COLLECTION_REQUEST("EntityCreateCollectionRequest"),
    ENTITY_UPDATE_REQUEST("EntityUpdateRequest"),
    ENTITY_UPDATE_COLLECTION_REQUEST("EntityUpdateCollectionRequest"),
    ENTITY_TRANSACTION_RESPONSE("EntityTransactionResponse"),
    ENTITY_DELETE_REQUEST("EntityDeleteRequest"),
    ENTITY_DELETE_RESPONSE("EntityDeleteResponse"),
    ENTITY_DELETE_ALL_REQUEST("EntityDeleteAllRequest"),
    ENTITY_DELETE_ALL_RESPONSE("EntityDeleteAllResponse"),
    ENTITY_TRANSITION_REQUEST("EntityTransitionRequest"),
    ENTITY_TRANSITION_RESPONSE("EntityTransitionResponse"),
    ENTITY_GET_REQUEST("EntityGetRequest"),
    ENTITY_GET_ALL_REQUEST("EntityGetAllRequest"),
    ENTITY_SNAPSHOT_SEARCH_REQUEST("EntitySnapshotSearchRequest"),
    ENTITY_SNAPSHOT_SEARCH_RESPONSE("EntitySnapshotSearchResponse"),
    ENTITY_RESPONSE("EntityResponse"),
    SNAPSHOT_CANCEL_REQUEST("SnapshotCancelRequest"),
    SNAPSHOT_GET_REQUEST("SnapshotGetRequest"),
    SNAPSHOT_GET_STATUS_REQUEST("SnapshotGetStatusRequest"),
    ENTITY_SEARCH_REQUEST("EntitySearchRequest"),
    ENTITY_STATS_GET_REQUEST("EntityStatsGetRequest"),
    ENTITY_STATS_RESPONSE("EntityStatsResponse"),
    ENTITY_STATS_BY_STATE_GET_REQUEST("EntityStatsByStateGetRequest"),
    ENTITY_STATS_BY_STATE_RESPONSE("EntityStatsByStateResponse"),
    ENTITY_CHANGES_METADATA_GET_REQUEST("EntityChangesMetadataGetRequest"),
    ENTITY_CHANGES_METADATA_RESPONSE("EntityChangesMetadataResponse");
    private final String value;
    private final static Map<String, CloudEventType> CONSTANTS = new HashMap<String, CloudEventType>();

    static {
        for (CloudEventType c: values()) {
            CONSTANTS.put(c.value, c);
        }
    }

    CloudEventType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    @JsonValue
    public String value() {
        return this.value;
    }

    @JsonCreator
    public static CloudEventType fromValue(String value) {
        CloudEventType constant = CONSTANTS.get(value);
        if (constant == null) {
            throw new IllegalArgumentException(value);
        } else {
            return constant;
        }
    }

}
