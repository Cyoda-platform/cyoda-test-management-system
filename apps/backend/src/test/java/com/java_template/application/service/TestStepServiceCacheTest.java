package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.config.CacheConfig;
import com.java_template.application.dto.TestStepDTO;
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

@SpringBootTest(classes = {TestStepService.class, CacheConfig.class})
public class TestStepServiceCacheTest {

    @MockBean EntityService entityService;
    @MockBean ObjectMapper objectMapper;
    @Autowired TestStepService testStepService;

    private <T extends CyodaEntity> EntityWithMetadata<T> wrap(T dto, UUID id) {
        EntityMetadata meta = new EntityMetadata();
        meta.setId(id);
        return new EntityWithMetadata<>(dto, meta);
    }

    @Test
    public void getTestStepsByTestCaseId_hitsCacheOnSecondCall() {
        UUID caseId = UUID.randomUUID();
        TestStepDTO step = new TestStepDTO();
        step.setTestCaseId(caseId);
        PageResult<EntityWithMetadata<TestStepDTO>> page =
                PageResult.of(null, List.of(wrap(step, UUID.randomUUID())), 0, 1000, 1L);
        when(entityService.search(any(), any(), eq(TestStepDTO.class))).thenReturn(page);

        testStepService.getTestStepsByTestCaseId(caseId);
        testStepService.getTestStepsByTestCaseId(caseId);

        verify(entityService, times(1)).search(any(), any(), eq(TestStepDTO.class));
    }
}
