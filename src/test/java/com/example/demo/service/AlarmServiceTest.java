package com.example.demo.service;

import com.example.demo.model.Alarm;
import com.example.demo.model.Alert;
import com.example.demo.model.Device;
import com.example.demo.model.Patient;
import com.example.demo.model.dto.AlarmQueryCondition;
import com.example.demo.model.dto.AlarmStatsDto;
import com.example.demo.repository.AlarmRepository;
import com.example.demo.repository.DeviceRepository;
import com.example.demo.repository.PatientRepository;
import com.example.demo.repository.AlertRepository;
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

/**
 * AlarmService单元测试
 */
@ExtendWith(MockitoExtension.class)
class AlarmServiceTest {

    @Mock private AlarmRepository alarmRepository;
    @Mock private TiandituLocationService tiandituLocationService;
    @Mock private DeviceRepository deviceRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private AlertRepository alertRepository;

    private AlarmService alarmService;

    @BeforeEach
    void setUp() {
        alarmService = new AlarmService(alarmRepository, tiandituLocationService, deviceRepository, patientRepository, alertRepository);
    }

    /**
     * 创建报警有坐标时调用天地图API获取地址
     */
    @Test
    void createAlarm_withCoordinates_fetchesAddress() {
        Alarm alarm = new Alarm();
        alarm.setLatitude(39.9087);
        alarm.setLongitude(116.3975);
        when(tiandituLocationService.regeoAddress(39.9087, 116.3975)).thenReturn("北京市东城区");
        Alarm saved = new Alarm();
        saved.setId(1L);
        when(alarmRepository.save(any(Alarm.class))).thenReturn(saved);
        Alert savedAlert = new Alert();
        savedAlert.setId(1L);
        when(alertRepository.save(any(Alert.class))).thenReturn(savedAlert);

        Alarm result = alarmService.createAlarm(alarm);

        verify(tiandituLocationService).regeoAddress(39.9087, 116.3975);
        assertEquals("北京市东城区", alarm.getAddress());
    }

    /**
     * 创建报警无坐标时不调用天地图API
     */
    @Test
    void createAlarm_withoutCoordinates_skipsAddressFetch() {
        Alarm alarm = new Alarm();
        Alarm saved = new Alarm();
        saved.setId(1L);
        when(alarmRepository.save(any(Alarm.class))).thenReturn(saved);
        Alert savedAlert = new Alert();
        savedAlert.setId(1L);
        when(alertRepository.save(any(Alert.class))).thenReturn(savedAlert);

        alarmService.createAlarm(alarm);

        verify(tiandituLocationService, never()).regeoAddress(anyDouble(), anyDouble());
    }

    /**
     * getAlarmWithRelations填充设备和病人信息
     */
    @Test
    void getAlarmWithRelations_fillsDeviceAndPatient() {
        Alarm alarm = new Alarm();
        alarm.setDeviceId(1L);
        alarm.setPatientId(10L);
        when(alarmRepository.findById(1L)).thenReturn(Optional.of(alarm));
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(new Device()));
        when(patientRepository.findById(10L)).thenReturn(Optional.of(new Patient()));

        Optional<Alarm> result = alarmService.getAlarmWithRelations(1L);

        assertTrue(result.isPresent());
        assertNotNull(result.get().getDevice());
        assertNotNull(result.get().getPatient());
    }

    /**
     * queryAlarms按设备ID+状态查询
     */
    @Test
    void queryAlarms_byDeviceIdAndStatus() {
        AlarmQueryCondition cond = new AlarmQueryCondition();
        cond.setDeviceId(1L);
        cond.setStatus("pending");
        cond.setPage(0);
        cond.setSize(20);
        when(alarmRepository.findByDeviceIdAndStatusOrderByTriggeredTimeDesc(eq(1L), eq("pending"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<Alarm> result = alarmService.queryAlarms(cond);

        assertNotNull(result);
        verify(alarmRepository).findByDeviceIdAndStatusOrderByTriggeredTimeDesc(eq(1L), eq("pending"), any(PageRequest.class));
    }

    /**
     * queryAlarms按状态查询
     */
    @Test
    void queryAlarms_byStatus() {
        AlarmQueryCondition cond = new AlarmQueryCondition();
        cond.setStatus("pending");
        cond.setPage(0);
        cond.setSize(20);
        when(alarmRepository.findByStatusOrderByTriggeredTimeDesc(eq("pending"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        alarmService.queryAlarms(cond);

        verify(alarmRepository).findByStatusOrderByTriggeredTimeDesc(eq("pending"), any(PageRequest.class));
    }

    /**
     * queryAlarms无条件查询全部
     */
    @Test
    void queryAlarms_noFilter_queriesAll() {
        AlarmQueryCondition cond = new AlarmQueryCondition();
        cond.setPage(0);
        cond.setSize(20);
        when(alarmRepository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));

        alarmService.queryAlarms(cond);

        verify(alarmRepository).findAll(any(PageRequest.class));
    }

    /**
     * markAlarmAsRead成功
     */
    @Test
    void markAlarmAsRead_success() {
        when(alarmRepository.markRead(1L, true)).thenReturn(1);
        assertTrue(alarmService.markAlarmAsRead(1L, true));
    }

    /**
     * markAlarmAsRead失败
     */
    @Test
    void markAlarmAsRead_notFound() {
        when(alarmRepository.markRead(999L, true)).thenReturn(0);
        assertFalse(alarmService.markAlarmAsRead(999L, true));
    }

    /**
     * getAlarmStats返回正确的统计数据
     */
    @Test
    void getAlarmStats_returnsCorrectData() {
        when(alarmRepository.count()).thenReturn(100L);
        when(alarmRepository.countByStatus("pending")).thenReturn(30L);
        when(alarmRepository.countByStatus("handled")).thenReturn(60L);
        when(alarmRepository.countByStatus("false_alarm")).thenReturn(10L);
        when(alarmRepository.countByIsReadFalse()).thenReturn(25L);

        AlarmStatsDto stats = alarmService.getAlarmStats();

        assertEquals(100L, stats.getTotal());
        assertEquals(30L, stats.getPending());
        assertEquals(60L, stats.getHandled());
        assertEquals(10L, stats.getFalseAlarm());
        assertEquals(25L, stats.getUnread());
    }

    /**
     * getUnreadAlarmCount调用正确方法
     */
    @Test
    void getUnreadAlarmCount() {
        when(alarmRepository.countByIsReadFalse()).thenReturn(5L);
        assertEquals(5L, alarmService.getUnreadAlarmCount());
    }

    /**
     * getUnreadAlarmCountByDeviceId调用正确方法
     */
    @Test
    void getUnreadAlarmCountByDeviceId() {
        when(alarmRepository.countByDeviceIdAndIsReadFalse(1L)).thenReturn(3L);
        assertEquals(3L, alarmService.getUnreadAlarmCountByDeviceId(1L));
    }

    /**
     * getAlarmsWithRelations填充关联设备和病人信息
     */
    @Test
    void getAlarmsWithRelations_fillsRelations() {
        AlarmQueryCondition cond = new AlarmQueryCondition();
        cond.setPage(0);
        cond.setSize(20);
        Alarm alarm = new Alarm();
        alarm.setDeviceId(1L);
        alarm.setPatientId(10L);
        Page<Alarm> page = new PageImpl<>(List.of(alarm));
        when(alarmRepository.findAll(any(PageRequest.class))).thenReturn(page);
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(new Device()));
        when(patientRepository.findById(10L)).thenReturn(Optional.of(new Patient()));

        Page<Alarm> result = alarmService.getAlarmsWithRelations(cond);

        assertNotNull(result.get().findFirst().get().getDevice());
        assertNotNull(result.get().findFirst().get().getPatient());
    }

    /**
     * handleAlarm成功后推送WebSocket更新
     */
    @Test
    void handleAlarm_success_pushesWebSocketUpdate() {
        Alarm updatedAlarm = new Alarm();
        updatedAlarm.setId(1L);
        when(alarmRepository.handleAlarm(eq(1L), anyString(), any(), any(), any(Date.class))).thenReturn(1);
        when(alarmRepository.findById(1L)).thenReturn(Optional.of(updatedAlarm));

        boolean result = alarmService.handleAlarm(1L, "handled", "已处理", null);

        assertTrue(result);
        verify(alarmRepository).findById(1L);
    }

    /**
     * markDeviceAlarmsAsRead成功
     */
    @Test
    void markDeviceAlarmsAsRead_success() {
        when(alarmRepository.markReadByDeviceId(1L, true)).thenReturn(3);
        assertTrue(alarmService.markDeviceAlarmsAsRead(1L, true));
    }

    /**
     * markPatientAlarmsAsRead成功
     */
    @Test
    void markPatientAlarmsAsRead_success() {
        when(alarmRepository.markReadByPatientId(10L, true)).thenReturn(5);
        assertTrue(alarmService.markPatientAlarmsAsRead(10L, true));
    }

    /**
     * getUnreadAlarmCountByPatientId调用正确方法
     */
    @Test
    void getUnreadAlarmCountByPatientId() {
        when(alarmRepository.countByPatientIdAndIsReadFalse(10L)).thenReturn(7L);
        assertEquals(7L, alarmService.getUnreadAlarmCountByPatientId(10L));
    }

    /**
     * getRecentAlarmsWithRelations限制数量
     */
    @Test
    void getRecentAlarmsWithRelations_limitsCount() {
        Alarm a1 = new Alarm();
        a1.setId(1L);
        a1.setDeviceId(1L);
        Alarm a2 = new Alarm();
        a2.setId(2L);
        a2.setDeviceId(1L);
        when(alarmRepository.findTop100ByOrderByTriggeredTimeDesc()).thenReturn(List.of(a1, a2));
        when(deviceRepository.findById(1L)).thenReturn(Optional.of(new Device()));

        List<Alarm> result = alarmService.getRecentAlarmsWithRelations(1);

        assertEquals(1, result.size());
        verify(deviceRepository).findById(1L);
    }

    // ========================================================================
    // Bug B 修复测试：清空 @Transient 关联字段以避免 WebSocket 序列化时
    // 触发 HibernateProxy 懒加载（Could not initialize proxy - no session）
    // ========================================================================

    @Test
    void clearTransientAssociations_nullifiesPatientAndDevice_keepsIds() {
        // Arrange - 模拟 Hibernate 加载后的 Alarm（含 patient/device 关联）
        Alarm alarm = new Alarm();
        alarm.setId(95L);
        alarm.setDeviceId(15L);
        alarm.setPatientId(7L);
        // 关键：@Transient 字段可能装着 HibernateProxy（未初始化状态）
        // 测试时不实际触发懒加载，只验证清空逻辑
        Patient patient = new Patient();
        patient.setId(7L);
        patient.setName("张三");
        Device device = new Device();
        device.setId(15L);
        device.setImei("355932600124999");
        alarm.setDevice(device);
        alarm.setPatient(patient);

        // 确认前置条件
        assertNotNull(alarm.getPatient(), "前置条件：patient 应被设置");
        assertNotNull(alarm.getDevice(), "前置条件：device 应被设置");

        // Act
        AlarmService.clearTransientAssociations(alarm);

        // Assert - @Transient 字段被清空
        assertNull(alarm.getPatient(), "patient @Transient 字段应被清空");
        assertNull(alarm.getDevice(), "device @Transient 字段应被清空");
        // ID 字段保留（Jackson 序列化时仍能输出）
        assertEquals(7L, alarm.getPatientId(), "patientId 必须保留");
        assertEquals(15L, alarm.getDeviceId(), "deviceId 必须保留");
        assertEquals(95L, alarm.getId(), "id 必须保留");
    }

    @Test
    void clearTransientAssociations_handlesNullGracefully() {
        // Arrange - Alarm 没有关联字段（@Transient 字段为 null）
        Alarm alarm = new Alarm();
        alarm.setId(1L);
        alarm.setDeviceId(15L);

        // Act & Assert - 不应抛 NullPointerException
        assertDoesNotThrow(() -> AlarmService.clearTransientAssociations(alarm));
        assertNull(alarm.getPatient());
        assertNull(alarm.getDevice());
        assertEquals(15L, alarm.getDeviceId());
    }

    @Test
    void clearTransientAssociations_simulationOfHibernateProxy() {
        // Arrange - 模拟 HibernateProxy 场景：传入的对象可能在序列化时
        // 才被访问字段（懒加载触发）。此测试不实际触发懒加载，
        // 只验证修复方法的逻辑正确性。
        //
        // 真实场景：FenceService.createFenceBreachAlert 调用
        //   createFenceBreachAlert(device, pd.getPatient(), fence, curr)
        // pd.getPatient() 返回 PatientDevice 中的 LAZY 关联（可能是 HibernateProxy）
        // 然后 alarm.setPatient(patient) - Proxy 装进 alarm
        // 事务提交后 WebSocketHandler.pushAlarm(savedAlarm) 异步序列化
        // → Jackson 访问 alarm.getPatient().getName() → 懒加载失败

        Alarm alarm = new Alarm();
        alarm.setDeviceId(15L);
        alarm.setPatientId(7L);

        // 模拟 HibernateProxy 状态：对象非空，但实际访问字段时会失败
        // 这里用普通对象代替（测试不依赖 Hibernate 容器）
        Patient proxyLikePatient = new Patient();
        proxyLikePatient.setId(7L);
        proxyLikePatient.setName("张三");
        alarm.setPatient(proxyLikePatient);

        // Act
        AlarmService.clearTransientAssociations(alarm);

        // Assert - 即使是 Proxy，清空后 Jackson 不会再访问其字段
        assertNull(alarm.getPatient());
        // ID 仍可用
        assertEquals(7L, alarm.getPatientId());
    }
}
