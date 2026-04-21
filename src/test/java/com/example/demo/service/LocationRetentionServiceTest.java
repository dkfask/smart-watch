package com.example.demo.service;

import com.example.demo.repository.LocationRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationRetentionServiceTest {

    @Mock private LocationRecordRepository repo;
    @Mock private JdbcTemplate jdbcTemplate;
    private LocationRetentionService service;

    @BeforeEach
    void setUp() {
        service = new LocationRetentionService(repo, jdbcTemplate, 30);
    }

    /**
     * scheduledCleanup 优先使用 DROP PARTITION，成功时不调用 deleteByRecvTimeBefore
     */
    @Test
    void scheduledCleanup_usesDropPartition_whenSuccessful() {
        service.scheduledCleanup();

        verify(jdbcTemplate).execute(contains("DROP PARTITION"));
        verify(repo, never()).deleteByRecvTimeBefore(any());
    }

    /**
     * DROP PARTITION 失败时回退到 DELETE 逻辑，先检查数量再删除
     */
    @Test
    void scheduledCleanup_fallsBackToDelete_whenDropPartitionFails() {
        doThrow(new RuntimeException("partition not found")).when(jdbcTemplate).execute(anyString());
        when(repo.countByRecvTimeBefore(any())).thenReturn(100L);

        service.scheduledCleanup();

        InOrder inOrder = inOrder(repo);
        inOrder.verify(repo).countByRecvTimeBefore(any());
        inOrder.verify(repo).deleteByRecvTimeBefore(any());
    }

    /**
     * retentionDays 为 0 时跳过清理，不执行任何操作
     */
    @Test
    void scheduledCleanup_skipsWhenRetentionDaysZero() {
        LocationRetentionService zeroService = new LocationRetentionService(repo, jdbcTemplate, 0);

        zeroService.scheduledCleanup();

        verifyNoInteractions(jdbcTemplate);
        verifyNoInteractions(repo);
    }

    /**
     * onApplicationReady 触发清理逻辑
     */
    @Test
    void onApplicationReady_triggersCleanup() {
        service.onApplicationReady();

        verify(jdbcTemplate).execute(contains("DROP PARTITION"));
    }
}
