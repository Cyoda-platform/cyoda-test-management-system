package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.config.CacheConfig;
import com.java_template.application.dto.TestRunDTO;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.dto.PageResult;
import com.java_template.common.service.EntityService;
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

@SpringBootTest(classes = {TestRunService.class, CacheConfig.class})
public class TestRunServiceCacheTest {

    @MockBean
    private EntityService entityService;

    @MockBean
    private ProjectCounterService projectCounterService;

    @MockBean
    private ObjectMapper objectMapper;

    @Autowired
    private TestRunService testRunService;

    private EntityWithMetadata<TestRunDTO> wrap(TestRunDTO dto, UUID id) {
        EntityMetadata meta = new EntityMetadata();
        meta.setId(id);
        return new EntityWithMetadata<>(dto, meta);
    }

    @Test
    public void getTestRunsByProjectId_hitsCacheOnSecondCall() {
        UUID projectId = UUID.randomUUID();
        UUID runId     = UUID.randomUUID();

        TestRunDTO run = new TestRunDTO();
        run.setProjectId(projectId);
        run.setName("Run 1");

        PageResult<EntityWithMetadata<TestRunDTO>> page = PageResult.of(
                null, List.of(wrap(run, runId)), 0, 20, 1L);
        when(entityService.findAll(any(), eq(TestRunDTO.class))).thenReturn(page);

        testRunService.getTestRunsByProjectId(projectId, 0, 20);
        testRunService.getTestRunsByProjectId(projectId, 0, 20);

        // Cache hit on second call — findAll called only ONCE
        verify(entityService, times(1)).findAll(any(), eq(TestRunDTO.class));
    }
}
