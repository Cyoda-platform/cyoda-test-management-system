package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.ReportDTO;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.dto.PageResult;
import com.java_template.common.repository.SearchAndRetrievalParams;
import com.java_template.common.service.EntityService;
import org.cyoda.cloud.api.common.model.GroupConditionDto;
import org.cyoda.cloud.api.event.common.EntityMetadata;
import org.cyoda.cloud.api.event.common.ModelSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private EntityService entityService;

    @Mock
    private ProjectCounterService projectCounterService;

    @Spy
    private ObjectMapper objectMapper;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(entityService, projectCounterService, objectMapper);
    }

    private EntityWithMetadata<ReportDTO> entity(UUID projectId) {
        ReportDTO dto = new ReportDTO();
        dto.setProjectId(projectId);
        EntityMetadata meta = new EntityMetadata();
        meta.setId(UUID.randomUUID());
        return new EntityWithMetadata<>(dto, meta);
    }

    private PageResult<EntityWithMetadata<ReportDTO>> pageOf(List<EntityWithMetadata<ReportDTO>> items) {
        return PageResult.of(null, items, 0, 20, (long) items.size());
    }

    @Test
    @DisplayName("getReportsByProjectId — uses server-side search, not findAll")
    void getReportsByProjectId_usesSearch_notFindAll() {
        UUID projectId = UUID.randomUUID();
        when(entityService.search(any(ModelSpec.class), any(GroupConditionDto.class),
                eq(ReportDTO.class), any(SearchAndRetrievalParams.class)))
                .thenReturn(pageOf(List.of()));

        reportService.getReportsByProjectId(projectId, 0, 20);

        verify(entityService).search(any(ModelSpec.class), any(GroupConditionDto.class),
                eq(ReportDTO.class), any(SearchAndRetrievalParams.class));
        verify(entityService, never()).findAll(any(), eq(ReportDTO.class), any());
    }

    @Test
    @DisplayName("getReportsByProjectId — totalElements comes from server, not filtered count")
    void getReportsByProjectId_totalElementsFromServer() {
        UUID projectId = UUID.randomUUID();
        PageResult<EntityWithMetadata<ReportDTO>> serverResult =
                PageResult.of(null, List.of(entity(projectId)), 0, 20, 42L);
        when(entityService.search(any(ModelSpec.class), any(GroupConditionDto.class),
                eq(ReportDTO.class), any(SearchAndRetrievalParams.class)))
                .thenReturn(serverResult);

        PageResult<ReportDTO> result = reportService.getReportsByProjectId(projectId, 0, 20);

        assertThat(result.totalElements()).isEqualTo(42L);
    }

    @Test
    @DisplayName("getReportsByProjectId — search condition contains projectId")
    void getReportsByProjectId_conditionContainsProjectId() {
        UUID projectId = UUID.randomUUID();
        ArgumentCaptor<GroupConditionDto> conditionCaptor = ArgumentCaptor.forClass(GroupConditionDto.class);
        when(entityService.search(any(ModelSpec.class), conditionCaptor.capture(),
                eq(ReportDTO.class), any(SearchAndRetrievalParams.class)))
                .thenReturn(pageOf(List.of()));

        reportService.getReportsByProjectId(projectId, 0, 20);

        GroupConditionDto condition = conditionCaptor.getValue();
        assertThat(condition).isNotNull();
        String conditionStr = condition.toString();
        assertThat(conditionStr).contains(projectId.toString());
    }
}
