package com.example.demo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Method;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartitionMaintenanceServiceTest {

    @Mock private JdbcTemplate jdbcTemplate;
    private PartitionMaintenanceService service;

    @BeforeEach
    void setUp() {
        service = new PartitionMaintenanceService(jdbcTemplate);
    }

    /**
     * monthlyPartitionMaintenance 正常执行时，对4张配置表都执行了分区创建操作
     */
    @Test
    void monthlyPartitionMaintenance_正常执行_对所有表执行分区操作() {
        service.monthlyPartitionMaintenance();

        verify(jdbcTemplate).execute(contains("location_records"));
        verify(jdbcTemplate).execute(contains("heartbeat_records"));
        verify(jdbcTemplate).execute(contains("fence_alerts"));
        verify(jdbcTemplate).execute(contains("system_log"));
        verify(jdbcTemplate, times(4)).execute(anyString());
    }

    /**
     * 某张表分区操作失败时不中断其他表，后续表仍然被处理
     */
    @Test
    void monthlyPartitionMaintenance_某张表失败_不中断其他表() {
        doThrow(new RuntimeException("分区操作失败"))
                .when(jdbcTemplate).execute(contains("location_records"));

        service.monthlyPartitionMaintenance();

        verify(jdbcTemplate).execute(contains("heartbeat_records"));
        verify(jdbcTemplate).execute(contains("fence_alerts"));
        verify(jdbcTemplate).execute(contains("system_log"));
    }

    /**
     * 通过反射测试 isPartitionExpired：过期分区（月份早于截止月）应被识别为过期
     */
    @Test
    void isPartitionExpired_过期分区应被识别() throws Exception {
        LocalDate expiryMonth = LocalDate.of(2026, 4, 1);

        assertTrue(invokeIsPartitionExpired("p202603", expiryMonth));
        assertTrue(invokeIsPartitionExpired("p202512", expiryMonth));
        assertFalse(invokeIsPartitionExpired("p202604", expiryMonth));
        assertFalse(invokeIsPartitionExpired("p202605", expiryMonth));
    }

    /**
     * 通过反射测试 isPartitionExpired：非标准分区名（不以p开头或长度不为7）不应被识别为过期
     */
    @Test
    void isPartitionExpired_非标准分区名不应被识别为过期() throws Exception {
        LocalDate expiryMonth = LocalDate.of(2026, 4, 1);

        assertFalse(invokeIsPartitionExpired("x202604", expiryMonth));
        assertFalse(invokeIsPartitionExpired("p20264", expiryMonth));
        assertFalse(invokeIsPartitionExpired("p20260401", expiryMonth));
        assertFalse(invokeIsPartitionExpired("p999999", expiryMonth));
    }

    /**
     * 无过期分区时不执行 DROP PARTITION 操作
     */
    @Test
    void dropExpiredPartitions_无过期分区_不执行DROP操作() {
        service.monthlyPartitionMaintenance();

        verify(jdbcTemplate, never()).execute(contains("DROP PARTITION"));
    }

    /**
     * 反射调用私有方法 isPartitionExpired
     */
    private boolean invokeIsPartitionExpired(String partitionName, LocalDate expiryMonth) throws Exception {
        Method method = PartitionMaintenanceService.class.getDeclaredMethod(
                "isPartitionExpired", String.class, LocalDate.class);
        method.setAccessible(true);
        return (boolean) method.invoke(service, partitionName, expiryMonth);
    }
}
