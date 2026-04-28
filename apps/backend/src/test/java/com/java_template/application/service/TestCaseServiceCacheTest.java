package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.config.CacheConfig;
import com.java_template.application.dto.TestCaseDTO;
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

@SpringBootTest(classes = {TestCaseService.class, CacheConfig.class})
public class TestCaseServiceCacheTest {

    @MockBean EntityService entityService;
    @MockBean ObjectMapper objectMapper;
    @MockBean ProjectCounterService projectCounterService;
    @Autowired TestCaseService testCaseService;

    private <T extends CyodaEntity> EntityWithMetadata<T> wrap(T dto, UUID id) {
        EntityMetadata meta = new EntityMetadata();
        meta.setId(id);
        return new EntityWithMetadata<>(dto, meta);
    }

    @Test
    public void getTestCasesBySuiteId_hitsCacheOnSecondCall() {
        UUID suiteId = UUID.randomUUID();
        TestCaseDTO tc = new TestCaseDTO();
        tc.setSuiteId(suiteId);
        PageResult<EntityWithMetadata<TestCaseDTO>> page =
                PageResult.of(null, List.of(wrap(tc, UUID.randomUUID())), 0, 20, 1L);
        when(entityService.search(any(), any(), eq(TestCaseDTO.class), any())).thenReturn(page);

        testCaseService.getTestCasesBySuiteId(suiteId, 0, 20);
        testCaseService.getTestCasesBySuiteId(suiteId, 0, 20);

        verify(entityService, times(1)).search(any(), any(), eq(TestCaseDTO.class), any());
    }
}
