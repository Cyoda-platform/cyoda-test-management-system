package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.config.CacheConfig;
import com.java_template.application.dto.TestRunCaseDTO;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.dto.PageResult;
import com.java_template.common.service.EntityService;
import com.java_template.common.workflow.CyodaEntity;
import org.cyoda.cloud.api.event.common.EntityMetadata;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {TestRunCaseService.class, CacheConfig.class})
public class TestRunCaseServiceCacheTest {

    @MockBean EntityService entityService;
    @MockBean ObjectMapper objectMapper;
    @MockBean TestRunStepService testRunStepService;
    @Autowired TestRunCaseService testRunCaseService;

    private <T extends CyodaEntity> EntityWithMetadata<T> wrap(T dto, UUID id) {
        EntityMetadata meta = new EntityMetadata();
        meta.setId(id);
        return new EntityWithMetadata<>(dto, meta);
    }

    @Test
    public void getAllTestRunCasesByTestRunId_hitsCacheOnSecondCall() {
        UUID runId = UUID.randomUUID();
        TestRunCaseDTO rc = new TestRunCaseDTO();
        PageResult<EntityWithMetadata<TestRunCaseDTO>> page =
                PageResult.of(null, List.of(wrap(rc, UUID.randomUUID())), 0, 1000, 1L);
        when(entityService.search(any(), any(), eq(TestRunCaseDTO.class))).thenReturn(page);

        testRunCaseService.getAllTestRunCasesByTestRunId(runId);
        testRunCaseService.getAllTestRunCasesByTestRunId(runId);

        verify(entityService, times(1)).search(any(), any(), eq(TestRunCaseDTO.class));
    }
}
