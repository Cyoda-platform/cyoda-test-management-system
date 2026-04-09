package com.java_template.common.grpc;

import com.java_template.common.grpc.client.connection.ConnectionManager;
import com.java_template.common.grpc.client.monitoring.GrpcConnectionMonitor;
import com.java_template.common.tool.CyodaInit;
import com.java_template.common.tool.CyodaInitConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GrpcAdminControllerTest {

    @Mock
    private ConnectionManager connectionManager;

    @Mock
    private GrpcConnectionMonitor connectionMonitor;

    @Mock
    private CyodaInit cyodaInit;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new GrpcAdminController(connectionManager, connectionMonitor, cyodaInit)
        ).build();
    }

    @Test
    void importWorkflowsReturnsOkWhenAllEntitiesSucceed() throws Exception {
        CyodaInit.ImportReport report = CyodaInit.ImportReport.fromResults(
                true,
                List.of(CyodaInit.EntityImportResult.success("Defect", 1, "workflow/defect/version_1/Defect.json", "ok"))
        );
        when(cyodaInit.initCyoda(any(CyodaInitConfig.class))).thenReturn(report);

        mockMvc.perform(post("/admin/grpc/import-workflows").param("recreateModels", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.recreateModels").value(true))
                .andExpect(jsonPath("$.succeededEntities").value(1))
                .andExpect(jsonPath("$.failedEntities").value(0))
                .andExpect(jsonPath("$.results[0].entityName").value("Defect"));

        ArgumentCaptor<CyodaInitConfig> captor = ArgumentCaptor.forClass(CyodaInitConfig.class);
        verify(cyodaInit).initCyoda(captor.capture());
        assertTrue(captor.getValue().recreateModels());
    }

    @Test
    void importWorkflowsReturnsMultiStatusWhenPartialFailuresOccur() throws Exception {
        CyodaInit.ImportReport report = CyodaInit.ImportReport.fromResults(
                false,
                List.of(
                        CyodaInit.EntityImportResult.success("Report", 1, "workflow/report/version_1/Report.json", "ok"),
                        CyodaInit.EntityImportResult.failure("TestRunCase", 1, "workflow/testruncase/version_1/TestRunCase.json", "delete failed")
                )
        );
        when(cyodaInit.initCyoda(any(CyodaInitConfig.class))).thenReturn(report);

        mockMvc.perform(post("/admin/grpc/import-workflows"))
                .andExpect(status().is(207))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.failedEntities").value(1))
                .andExpect(jsonPath("$.message", containsString("failures")))
                .andExpect(jsonPath("$.results[1].entityName").value("TestRunCase"))
                .andExpect(jsonPath("$.results[1].success").value(false));
    }

    @Test
    void importWorkflowsReturnsInternalServerErrorWhenInitThrows() throws Exception {
        when(cyodaInit.initCyoda(any(CyodaInitConfig.class))).thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(post("/admin/grpc/import-workflows").param("recreateModels", "true"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.recreateModels").value(true))
                .andExpect(jsonPath("$.message", containsString("boom")));
    }
}
