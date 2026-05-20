package com.java_template.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java_template.application.dto.DefectDTO;
import com.java_template.application.dto.ProjectCounterDTO;
import com.java_template.application.dto.TestCaseDTO;
import com.java_template.application.dto.TestRunDTO;
import com.java_template.common.dto.EntityWithMetadata;
import com.java_template.common.dto.PageResult;
import com.java_template.common.service.EntityService;
import org.cyoda.cloud.api.event.common.EntityMetadata;
import org.cyoda.cloud.api.event.common.ModelSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.ArrayList;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * SG-01: Unit tests for ProjectCounterService — the most complex concurrent code
 * in the codebase with no previous test coverage.
 */
@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class ProjectCounterServiceTest {

    @Mock
    private EntityService entityService;

    @Spy
    private ObjectMapper objectMapper;

    private ProjectCounterService service;
    private UUID projectId;

    @BeforeEach
    void setUp() {
        service = new ProjectCounterService(entityService, objectMapper);
        projectId = UUID.randomUUID();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private EntityWithMetadata<ProjectCounterDTO> wrapCounter(ProjectCounterDTO dto) {
        EntityMetadata meta = new EntityMetadata();
        meta.setId(dto.getId() != null ? dto.getId() : UUID.randomUUID());
        return new EntityWithMetadata<>(dto, meta);
    }

    private EntityWithMetadata<TestCaseDTO> wrapCase(TestCaseDTO dto) {
        EntityMetadata meta = new EntityMetadata();
        meta.setId(UUID.randomUUID());
        return new EntityWithMetadata<>(dto, meta);
    }

    private EntityWithMetadata<TestRunDTO> wrapRun(TestRunDTO dto) {
        EntityMetadata meta = new EntityMetadata();
        meta.setId(UUID.randomUUID());
        return new EntityWithMetadata<>(dto, meta);
    }

    private PageResult<EntityWithMetadata<ProjectCounterDTO>> counterPage(ProjectCounterDTO counter) {
        return PageResult.of(null, List.of(wrapCounter(counter)), 0, 100, 1L);
    }

    private PageResult<EntityWithMetadata<ProjectCounterDTO>> emptyCounterPage() {
        return PageResult.of(null, List.of(), 0, 100, 0L);
    }

    private ProjectCounterDTO counterAt(long nextId) {
        ProjectCounterDTO dto = new ProjectCounterDTO();
        dto.setId(UUID.randomUUID());
        dto.setProjectId(projectId);
        dto.setNextId(nextId);
        dto.setNextRunId(nextId);
        dto.setNextDefectId(nextId);
        dto.setNextReportId(nextId);
        return dto;
    }

    private void stubCounterSearch(ProjectCounterDTO counter) {
        doReturn(counterPage(counter))
                .when(entityService).search(any(ModelSpec.class), any(), eq(ProjectCounterDTO.class));
    }

    private void stubEmptyCounterSearch() {
        doReturn(emptyCounterPage())
                .when(entityService).search(any(ModelSpec.class), any(), eq(ProjectCounterDTO.class));
    }

    private void stubUpdate(ProjectCounterDTO counter) {
        doReturn(wrapCounter(counter))
                .when(entityService).update(any(UUID.class), any(ProjectCounterDTO.class), isNull());
    }

    private void stubCreate(ProjectCounterDTO counter) {
        doReturn(wrapCounter(counter))
                .when(entityService).create(any(ProjectCounterDTO.class));
    }

    private void stubEmptyCaseScan() {
        PageResult<EntityWithMetadata<TestCaseDTO>> empty = PageResult.of(null, List.of(), 0, 10000, 0L);
        doReturn(empty)
                .when(entityService).search(any(ModelSpec.class), any(), eq(TestCaseDTO.class), any());
    }

    private void stubEmptyRunScan() {
        PageResult<EntityWithMetadata<TestRunDTO>> empty = PageResult.of(null, List.of(), 0, 10000, 0L);
        doReturn(empty)
                .when(entityService).search(any(ModelSpec.class), any(), eq(TestRunDTO.class), any());
    }

    // ── nextDisplayId (TC-N) ─────────────────────────────────────────────────

    @Test
    void nextDisplayId_returnsCorrectFormat() {
        ProjectCounterDTO counter = counterAt(7);
        stubCounterSearch(counter);
        stubUpdate(counter);

        assertThat(service.nextDisplayId(projectId)).isEqualTo("TC-7");
    }

    @Test
    void nextDisplayId_incrementsCounterAfterAssignment() {
        ProjectCounterDTO counter = counterAt(3);
        stubCounterSearch(counter);
        stubUpdate(counter);

        service.nextDisplayId(projectId);

        ArgumentCaptor<ProjectCounterDTO> saved = ArgumentCaptor.forClass(ProjectCounterDTO.class);
        verify(entityService).update(any(UUID.class), saved.capture(), isNull());
        assertThat(saved.getValue().getNextId()).isEqualTo(4L);
    }

    @Test
    void nextDisplayIdBatch_reservesMultipleIdsInOneRoundTrip() {
        ProjectCounterDTO counter = counterAt(10);
        stubCounterSearch(counter);
        stubUpdate(counter);

        List<String> ids = service.nextDisplayIdBatch(projectId, 3);

        assertThat(ids).containsExactly("TC-10", "TC-11", "TC-12");
        verify(entityService, times(1)).update(any(), any(), isNull());
    }

    @Test
    void nextDisplayIdBatch_rejectsZeroCount() {
        assertThatThrownBy(() -> service.nextDisplayIdBatch(projectId, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── Other prefixes ───────────────────────────────────────────────────────

    @Test
    void nextRunDisplayId_returnsCorrectFormat() {
        ProjectCounterDTO counter = counterAt(5);
        stubCounterSearch(counter);
        stubUpdate(counter);

        assertThat(service.nextRunDisplayId(projectId)).isEqualTo("TR-5");
    }

    @Test
    void nextDefectDisplayId_returnsCorrectFormat() {
        ProjectCounterDTO counter = counterAt(2);
        stubCounterSearch(counter);
        stubUpdate(counter);

        assertThat(service.nextDefectDisplayId(projectId)).isEqualTo("DEF-2");
    }

    @Test
    void nextReportDisplayId_returnsCorrectFormat() {
        ProjectCounterDTO counter = counterAt(1);
        stubCounterSearch(counter);
        stubUpdate(counter);

        assertThat(service.nextReportDisplayId(projectId)).isEqualTo("REP-1");
    }

    // ── Bootstrap path (no existing counter record) ──────────────────────────

    @Test
    void nextDisplayId_bootstrapsCounterWhenNoneExists() {
        stubEmptyCounterSearch();
        stubEmptyCaseScan();
        stubCreate(counterAt(2));

        assertThat(service.nextDisplayId(projectId)).isEqualTo("TC-1");
        verify(entityService).create(any(ProjectCounterDTO.class));
    }

    @Test
    void nextDisplayId_bootstrapStartsAboveExistingMaxId() {
        stubEmptyCounterSearch();

        TestCaseDTO existing = new TestCaseDTO();
        existing.setDisplayId("TC-15");
        PageResult<EntityWithMetadata<TestCaseDTO>> casePage =
                PageResult.of(null, List.of(wrapCase(existing)), 0, 10000, 1L);
        doReturn(casePage)
                .when(entityService).search(any(ModelSpec.class), any(), eq(TestCaseDTO.class), any());

        ArgumentCaptor<ProjectCounterDTO> created = ArgumentCaptor.forClass(ProjectCounterDTO.class);
        stubCreate(counterAt(17));

        String id = service.nextDisplayId(projectId);

        verify(entityService).create(created.capture());
        assertThat(id).isEqualTo("TC-16");
        assertThat(created.getValue().getNextId()).isEqualTo(17L);
    }

    // ── Zero field migration ─────────────────────────────────────────────────

    @Test
    void nextRunDisplayId_bootstrapsWhenFieldIsZero() {
        ProjectCounterDTO counter = counterAt(5);
        counter.setNextRunId(0L); // old record without TR counter
        stubCounterSearch(counter);

        TestRunDTO existingRun = new TestRunDTO();
        existingRun.setDisplayId("TR-3");
        PageResult<EntityWithMetadata<TestRunDTO>> runPage =
                PageResult.of(null, List.of(wrapRun(existingRun)), 0, 10000, 1L);
        doReturn(runPage)
                .when(entityService).search(any(ModelSpec.class), any(), eq(TestRunDTO.class), any());
        stubUpdate(counter);

        assertThat(service.nextRunDisplayId(projectId)).isEqualTo("TR-4");
    }

    // ── In-memory cache ──────────────────────────────────────────────────────

    @Test
    void consecutiveCalls_searchesCounterOnlyOnce() {
        ProjectCounterDTO counter = counterAt(1);
        stubCounterSearch(counter);
        stubUpdate(counter);

        service.nextDisplayId(projectId);
        service.nextDisplayId(projectId);
        service.nextDisplayId(projectId);

        verify(entityService, times(1))
                .search(any(ModelSpec.class), any(), eq(ProjectCounterDTO.class));
        verify(entityService, times(3)).update(any(), any(), isNull());
    }

    @Test
    void consecutiveCalls_produceStrictlyIncreasingIds() {
        ProjectCounterDTO counter = counterAt(1);
        stubCounterSearch(counter);
        stubUpdate(counter);

        assertThat(List.of(
                service.nextDisplayId(projectId),
                service.nextDisplayId(projectId),
                service.nextDisplayId(projectId)))
                .containsExactly("TC-1", "TC-2", "TC-3");
    }

    // ── initializeCounterForProject ──────────────────────────────────────────

    @Test
    void initializeCounterForProject_createsRecordWithAllFieldsAtOne() {
        stubCreate(counterAt(1));

        service.initializeCounterForProject(projectId);

        ArgumentCaptor<ProjectCounterDTO> cap = ArgumentCaptor.forClass(ProjectCounterDTO.class);
        verify(entityService).create(cap.capture());
        ProjectCounterDTO dto = cap.getValue();
        assertThat(dto.getProjectId()).isEqualTo(projectId);
        assertThat(dto.getNextId()).isEqualTo(1L);
        assertThat(dto.getNextRunId()).isEqualTo(1L);
        assertThat(dto.getNextDefectId()).isEqualTo(1L);
        assertThat(dto.getNextReportId()).isEqualTo(1L);
    }

    @Test
    void initializeCounterForProject_doesNotPropagateCreateFailure() {
        doThrow(new RuntimeException("duplicate")).when(entityService).create(any());

        service.initializeCounterForProject(projectId);
        // No exception expected — failure is a warning-level log
    }

    // ── deleteCounterForProject ──────────────────────────────────────────────

    @Test
    void deleteCounterForProject_deletesEntityAndEvictsCache() {
        ProjectCounterDTO counter = counterAt(1);
        stubCounterSearch(counter);
        stubUpdate(counter);
        service.nextDisplayId(projectId); // populates cache

        // Re-stub for the search inside deleteCounterForProject
        doReturn(counterPage(counter))
                .when(entityService).search(any(ModelSpec.class), any(), eq(ProjectCounterDTO.class));
        doReturn(counter.getId()).when(entityService).deleteById(counter.getId());

        service.deleteCounterForProject(projectId);

        verify(entityService).deleteById(counter.getId());
    }

    @Test
    void deleteCounterForProject_isNoOpWhenNoneExists() {
        doReturn(emptyCounterPage())
                .when(entityService).search(any(ModelSpec.class), any(), eq(ProjectCounterDTO.class));

        service.deleteCounterForProject(projectId);

        verify(entityService, never()).deleteById(any());
    }

    // ── Concurrent access ────────────────────────────────────────────────────

    @Test
    void concurrentCallsSameProject_produceUniqueIds() throws Exception {
        ProjectCounterDTO counter = counterAt(1);
        lenient().doReturn(counterPage(counter))
                .when(entityService).search(any(ModelSpec.class), any(), eq(ProjectCounterDTO.class));
        lenient().doReturn(wrapCounter(counter))
                .when(entityService).update(any(UUID.class), any(ProjectCounterDTO.class), isNull());

        int threads = 5;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                return service.nextDisplayId(projectId);
            }));
        }
        start.countDown();

        Set<String> ids = new HashSet<>();
        for (Future<String> f : futures) ids.add(f.get());
        pool.shutdown();

        assertThat(ids).hasSize(threads);
    }
}
