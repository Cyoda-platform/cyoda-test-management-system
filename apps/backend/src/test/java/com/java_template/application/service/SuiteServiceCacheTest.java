package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.config.CacheConfig;
import com.java_template.application.dto.SuiteDTO;
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

@SpringBootTest(classes = {SuiteService.class, CacheConfig.class})
public class SuiteServiceCacheTest {

    @MockBean EntityService entityService;
    @MockBean ObjectMapper objectMapper;
    @MockBean TestCaseService testCaseService;
    @Autowired SuiteService suiteService;

    private <T extends CyodaEntity> EntityWithMetadata<T> wrap(T dto, UUID id) {
        EntityMetadata meta = new EntityMetadata();
        meta.setId(id);
        return new EntityWithMetadata<>(dto, meta);
    }

    @Test
    public void getAllSuitesByProjectId_hitsCacheOnSecondCall() {
        UUID projectId = UUID.randomUUID();
        SuiteDTO suite = new SuiteDTO();
        suite.setProjectId(projectId);
        PageResult<EntityWithMetadata<SuiteDTO>> page =
                PageResult.of(null, List.of(wrap(suite, UUID.randomUUID())), 0, 1000, 1L);
        when(entityService.search(any(), any(), eq(SuiteDTO.class))).thenReturn(page);

        suiteService.getAllSuitesByProjectId(projectId);
        suiteService.getAllSuitesByProjectId(projectId);

        verify(entityService, times(1)).search(any(), any(), eq(SuiteDTO.class));
    }
}
