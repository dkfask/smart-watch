package com.example.demo.service;

import com.example.demo.model.Alert;
import com.example.demo.model.Device;
import com.example.demo.model.Patient;
import com.example.demo.model.dto.AlarmQueryCondition;
import com.example.demo.model.dto.AlarmStatsDto;
import com.example.demo.repository.AlertRepository;
import com.example.demo.repository.DeviceRepository;
import com.example.demo.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock private AlertRepository alertRepository;
    @Mock private TiandituLocationService tiandituLocationService;
    @Mock private DeviceRepository deviceRepository;
    @Mock private PatientRepository patientRepository;

    private AlertService alertService;

    @BeforeEach
    void setUp() {
        alertService = new AlertService(alertRepository, tiandituLocationService, deviceRepository, patientRepository);
    }

    /**
     * 创建报警有坐标时调用天地图API获取地址并设置
     */
    @Test
    void createAlert_withCoordinates_fetchesAddress() {
        Alert alert = new Alert();
        alert.setLatitude(39.9087);
        alert.setLongitude(116.3975);
        when(tiandituLocationService.regeoAddress(39.9087, 116.3975)).thenReturn("北京市东城区");
        Alert saved = new Alert();
        saved.setId(1L);
        when(alertRepository.save(any(Alert.class))).thenReturn(saved);

        Alert result = alertService.createAlert(alert);

        verify(tiandituLocationService).regeoAddress(39.9087, 116.3975);
        assertEquals("北京市东城区", alert.getAddress());
        assertNotNull(result);
    }

    /**
     * 创建报警无坐标时不调用天地图API
     */
    @Test
    void createAlert_withoutCoordinates_doesNotFetchAddress() {
        Alert alert = new Alert();
        alert.setLatitude(null);
        alert.setLongitude(null);
        when(alertRepository.save(any(Alert.class))).thenReturn(alert);

        alertService.createAlert(alert);

        verify(tiandituLocationService, never()).regeoAddress(anyDouble(), anyDouble());
    }

    /**
     * 根据ID获取报警存在时填充设备和病人关联信息
     */
    @Test
    void getAlertWithRelations_fillsDeviceAndPatient() {
        Alert alert = new Alert();
        alert.setDeviceId(1L);
        alert.setPatientId(10L);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(alert));
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(new Device()));
        when(patientRepository.findById(10L)).thenReturn(Optional.of(new Patient()));

        Optional<Alert> result = alertService.getAlertWithRelations(1L);

        assertTrue(result.isPresent());
        assertNotNull(result.get().getDevice());
        assertNotNull(result.get().getPatient());
    }

    /**
     * 根据ID获取报警不存在时返回Optional.empty
     */
    @Test
    void getAlertWithRelations_notFound_returnsEmpty() {
        when(alertRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Alert> result = alertService.getAlertWithRelations(999L);

        assertFalse(result.isPresent());
    }

    /**
     * 获取报警列表含关联信息时填充每条记录的设备和病人
     */
    @Test
    void getAlertsWithRelations_fillsRelations() {
        AlarmQueryCondition cond = new AlarmQueryCondition();
        cond.setPage(0);
        cond.setSize(20);
        Alert alert = new Alert();
        alert.setDeviceId(1L);
        alert.setPatientId(10L);
        Page<Alert> page = new PageImpl<>(List.of(alert));
        when(alertRepository.findAll(any(PageRequest.class))).thenReturn(page);
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(new Device()));
        when(patientRepository.findById(10L)).thenReturn(Optional.of(new Patient()));

        Page<Alert> result = alertService.getAlertsWithRelations(cond);

        assertNotNull(result.getContent().get(0).getDevice());
        assertNotNull(result.getContent().get(0).getPatient());
    }

    /**
     * 按设备ID和状态查询报警
     */
    @Test
    void queryAlerts_byDeviceIdAndStatus() {
        AlarmQueryCondition cond = new AlarmQueryCondition();
        cond.setDeviceId(1L);
        cond.setStatus("pending");
        cond.setPage(0);
        cond.setSize(20);
        when(alertRepository.findByDeviceIdAndStatusOrderByAlertTimeDesc(eq(1L), eq("pending"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<Alert> result = alertService.queryAlerts(cond);

        assertNotNull(result);
        verify(alertRepository).findByDeviceIdAndStatusOrderByAlertTimeDesc(eq(1L), eq("pending"), any(PageRequest.class));
    }

    /**
     * 按病人ID和状态查询报警
     */
    @Test
    void queryAlerts_byPatientIdAndStatus() {
        AlarmQueryCondition cond = new AlarmQueryCondition();
        cond.setPatientId(10L);
        cond.setStatus("pending");
        cond.setPage(0);
        cond.setSize(20);
        when(alertRepository.findByPatientIdAndStatusOrderByAlertTimeDesc(eq(10L), eq("pending"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<Alert> result = alertService.queryAlerts(cond);

        assertNotNull(result);
        verify(alertRepository).findByPatientIdAndStatusOrderByAlertTimeDesc(eq(10L), eq("pending"), any(PageRequest.class));
    }

    /**
     * 仅按设备ID查询报警
     */
    @Test
    void queryAlerts_byDeviceId() {
        AlarmQueryCondition cond = new AlarmQueryCondition();
        cond.setDeviceId(1L);
        cond.setPage(0);
        cond.setSize(20);
        when(alertRepository.findByDeviceIdOrderByAlertTimeDesc(eq(1L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<Alert> result = alertService.queryAlerts(cond);

        assertNotNull(result);
        verify(alertRepository).findByDeviceIdOrderByAlertTimeDesc(eq(1L), any(PageRequest.class));
    }

    /**
     * 仅按病人ID查询报警
     */
    @Test
    void queryAlerts_byPatientId() {
        AlarmQueryCondition cond = new AlarmQueryCondition();
        cond.setPatientId(10L);
        cond.setPage(0);
        cond.setSize(20);
        when(alertRepository.findByPatientIdOrderByAlertTimeDesc(eq(10L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<Alert> result = alertService.queryAlerts(cond);

        assertNotNull(result);
        verify(alertRepository).findByPatientIdOrderByAlertTimeDesc(eq(10L), any(PageRequest.class));
    }

    /**
     * 仅按状态查询报警
     */
    @Test
    void queryAlerts_byStatus() {
        AlarmQueryCondition cond = new AlarmQueryCondition();
        cond.setStatus("pending");
        cond.setPage(0);
        cond.setSize(20);
        when(alertRepository.findByStatusOrderByAlertTimeDesc(eq("pending"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<Alert> result = alertService.queryAlerts(cond);

        assertNotNull(result);
        verify(alertRepository).findByStatusOrderByAlertTimeDesc(eq("pending"), any(PageRequest.class));
    }

    /**
     * 按时间范围查询报警
     */
    @Test
    void queryAlerts_byTimeRange() {
        AlarmQueryCondition cond = new AlarmQueryCondition();
        Date start = new Date(System.currentTimeMillis() - 86400000L);
        Date end = new Date();
        cond.setStartTime(start);
        cond.setEndTime(end);
        cond.setPage(0);
        cond.setSize(20);
        when(alertRepository.findByAlertTimeBetweenOrderByAlertTimeDesc(eq(start), eq(end), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<Alert> result = alertService.queryAlerts(cond);

        assertNotNull(result);
        verify(alertRepository).findByAlertTimeBetweenOrderByAlertTimeDesc(eq(start), eq(end), any(PageRequest.class));
    }

    /**
     * 无条件查询全部报警
     */
    @Test
    void queryAlerts_noFilter_queriesAll() {
        AlarmQueryCondition cond = new AlarmQueryCondition();
        cond.setPage(0);
        cond.setSize(20);
        when(alertRepository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));

        alertService.queryAlerts(cond);

        verify(alertRepository).findAll(any(PageRequest.class));
    }

    /**
     * 获取最近报警限制返回数量并填充关联信息
     */
    @Test
    void getRecentAlertsWithRelations_limitsCount() {
        Alert a1 = new Alert();
        a1.setId(1L);
        a1.setDeviceId(1L);
        Alert a2 = new Alert();
        a2.setId(2L);
        a2.setDeviceId(1L);
        when(alertRepository.findTop100ByOrderByAlertTimeDesc()).thenReturn(List.of(a1, a2));
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(new Device()));

        List<Alert> result = alertService.getRecentAlertsWithRelations(1);

        assertEquals(1, result.size());
        verify(deviceRepository).findById(1L);
    }

    /**
     * 获取未读报警数量
     */
    @Test
    void getUnreadAlertCount() {
        when(alertRepository.countByIsReadNot(1)).thenReturn(5L);
        assertEquals(5L, alertService.getUnreadAlertCount());
    }

    /**
     * 获取指定设备的未读报警数量
     */
    @Test
    void getUnreadAlertCountByDeviceId() {
        when(alertRepository.countByDeviceIdAndIsReadNot(1L, 1)).thenReturn(3L);
        assertEquals(3L, alertService.getUnreadAlertCountByDeviceId(1L));
    }

    /**
     * 获取指定病人的未读报警数量
     */
    @Test
    void getUnreadAlertCountByPatientId() {
        when(alertRepository.countByPatientIdAndIsReadNot(10L, 1)).thenReturn(7L);
        assertEquals(7L, alertService.getUnreadAlertCountByPatientId(10L));
    }

    /**
     * 标记报警为已读成功
     */
    @Test
    void markAlertAsRead_success() {
        when(alertRepository.markRead(1L, 1)).thenReturn(1);
        assertTrue(alertService.markAlertAsRead(1L, 1));
    }

    /**
     * 标记报警为已读失败
     */
    @Test
    void markAlertAsRead_notFound() {
        when(alertRepository.markRead(999L, 1)).thenReturn(0);
        assertFalse(alertService.markAlertAsRead(999L, 1));
    }

    /**
     * 批量标记设备报警为已读成功
     */
    @Test
    void markDeviceAlertsAsRead_success() {
        when(alertRepository.markReadByDeviceId(1L, 1)).thenReturn(3);
        assertTrue(alertService.markDeviceAlertsAsRead(1L, 1));
    }

    /**
     * 批量标记设备报警为已读失败
     */
    @Test
    void markDeviceAlertsAsRead_notFound() {
        when(alertRepository.markReadByDeviceId(999L, 1)).thenReturn(0);
        assertFalse(alertService.markDeviceAlertsAsRead(999L, 1));
    }

    /**
     * 批量标记病人报警为已读成功
     */
    @Test
    void markPatientAlertsAsRead_success() {
        when(alertRepository.markReadByPatientId(10L, 1)).thenReturn(5);
        assertTrue(alertService.markPatientAlertsAsRead(10L, 1));
    }

    /**
     * 批量标记病人报警为已读失败
     */
    @Test
    void markPatientAlertsAsRead_notFound() {
        when(alertRepository.markReadByPatientId(999L, 1)).thenReturn(0);
        assertFalse(alertService.markPatientAlertsAsRead(999L, 1));
    }

    /**
     * 处理报警成功时推送WebSocket更新
     */
    @Test
    void handleAlert_success_pushesWebSocketUpdate() {
        Alert updatedAlert = new Alert();
        updatedAlert.setId(1L);
        when(alertRepository.handleAlert(eq(1L), anyString(), anyString(), anyString(), any(Date.class))).thenReturn(1);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(updatedAlert));

        boolean result = alertService.handleAlert(1L, "handled", "已处理", "备注");

        assertTrue(result);
        verify(alertRepository).findById(1L);
    }

    /**
     * 处理报警失败返回false
     */
    @Test
    void handleAlert_failure_returnsFalse() {
        when(alertRepository.handleAlert(eq(999L), anyString(), anyString(), anyString(), any(Date.class))).thenReturn(0);

        boolean result = alertService.handleAlert(999L, "handled", "已处理", "备注");

        assertFalse(result);
        verify(alertRepository, never()).findById(anyLong());
    }

    /**
     * 获取报警统计数据验证各字段正确性
     */
    @Test
    void getAlertStats_returnsCorrectData() {
        when(alertRepository.count()).thenReturn(100L);
        when(alertRepository.countByStatus("pending")).thenReturn(30L);
        when(alertRepository.countByStatus("handled")).thenReturn(60L);
        when(alertRepository.countByStatus("false_alarm")).thenReturn(10L);
        when(alertRepository.countByIsReadNot(1)).thenReturn(25L);

        AlarmStatsDto stats = alertService.getAlertStats();

        assertEquals(100L, stats.getTotal());
        assertEquals(30L, stats.getPending());
        assertEquals(60L, stats.getHandled());
        assertEquals(10L, stats.getFalseAlarm());
        assertEquals(25L, stats.getUnread());
    }
}
