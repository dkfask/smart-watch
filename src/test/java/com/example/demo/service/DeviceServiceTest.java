package com.example.demo.service;

import com.example.demo.model.Device;
import com.example.demo.model.DeviceStatus;
import com.example.demo.model.Patient;
import com.example.demo.model.PatientDevice;
import com.example.demo.model.dto.DeviceInfoDto;
import com.example.demo.model.dto.PageResponse;
import com.example.demo.repository.DeviceRepository;
import com.example.demo.repository.DeviceStatusRepository;
import com.example.demo.repository.PatientDeviceRepository;
import com.example.demo.repository.PatientRepository;
import com.example.demo.socket.downlink.DownlinkManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DeviceService单元测试
 */
@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock private DeviceRepository deviceRepo;
    @Mock private DeviceStatusRepository statusRepo;
    @Mock private PatientDeviceRepository patientDeviceRepo;
    @Mock private PatientRepository patientRepo;
    @Mock private DownlinkManager downlinkManager;
    @Mock private CacheService cacheService;

    private DeviceService deviceService;

    @BeforeEach
    void setUp() {
        deviceService = new DeviceService(deviceRepo, statusRepo, patientDeviceRepo, patientRepo, downlinkManager, cacheService);
    }

    /**
     * 创建设备保存并返回ID
     */
    @Test
    void createDevice_savesAndReturnsId() {
        Device device = new Device();
        device.setImei("IMEI001");
        Device saved = new Device();
        saved.setId(1L);
        saved.setImei("IMEI001");
        when(deviceRepo.save(any(Device.class))).thenReturn(saved);

        Long id = deviceService.createDevice(device);

        assertEquals(1L, id);
    }

    /**
     * existsById存在返回true
     */
    @Test
    void existsById_exists_returnsTrue() {
        when(deviceRepo.existsById(1L)).thenReturn(true);
        assertTrue(deviceService.existsById(1L));
    }

    /**
     * existsById不存在返回false
     */
    @Test
    void existsById_notExists_returnsFalse() {
        when(deviceRepo.existsById(999L)).thenReturn(false);
        assertFalse(deviceService.existsById(999L));
    }

    /**
     * 更新设备并清除缓存
     */
    @Test
    void updateDevice_updatesAndClearsCache() {
        Device device = new Device();
        device.setImei("IMEI001");
        when(deviceRepo.save(any(Device.class))).thenReturn(device);

        deviceService.updateDevice(1L, device);

        verify(deviceRepo).save(argThat(d -> d.getId() == 1L));
        verify(cacheService).delete(anyString());
    }

    /**
     * listDevices无搜索条件分页查询
     */
    @Test
    void listDevices_noSearch_returnsPagedResult() {
        Device d = new Device();
        d.setId(1L);
        d.setImei("IMEI001");
        Page<Device> page = new PageImpl<>(List.of(d));
        when(deviceRepo.findAll(any(Pageable.class))).thenReturn(page);
        when(statusRepo.findAllById(anyList())).thenReturn(Collections.emptyList());
        when(patientDeviceRepo.findByDeviceIdIn(anyList())).thenReturn(Collections.emptyList());
        when(downlinkManager.getOnlineImeis()).thenReturn(Collections.emptySet());

        PageResponse<DeviceInfoDto> result = deviceService.listDevices(0, 20, null);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("IMEI001", result.getContent().get(0).getImei());
    }

    /**
     * listDevices有搜索条件模糊搜索
     */
    @Test
    void listDevices_withSearch_callsFindBySearch() {
        Device d = new Device();
        d.setId(1L);
        d.setImei("IMEI001");
        Page<Device> page = new PageImpl<>(List.of(d));
        when(deviceRepo.findByImeiContainingOrIccidContainingOrImsiContaining(anyString(), anyString(), anyString(), any(Pageable.class)))
                .thenReturn(page);
        when(statusRepo.findAllById(anyList())).thenReturn(Collections.emptyList());
        when(patientDeviceRepo.findByDeviceIdIn(anyList())).thenReturn(Collections.emptyList());
        when(downlinkManager.getOnlineImeis()).thenReturn(Collections.emptySet());

        PageResponse<DeviceInfoDto> result = deviceService.listDevices(0, 20, "IMEI");

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    /**
     * listDevices设备在线状态正确
     */
    @Test
    void listDevices_onlineStatusCorrect() {
        Device d = new Device();
        d.setId(1L);
        d.setImei("ONLINE_IMEI");
        Page<Device> page = new PageImpl<>(List.of(d));
        when(deviceRepo.findAll(any(Pageable.class))).thenReturn(page);
        when(statusRepo.findAllById(anyList())).thenReturn(Collections.emptyList());
        when(patientDeviceRepo.findByDeviceIdIn(anyList())).thenReturn(Collections.emptyList());
        when(downlinkManager.getOnlineImeis()).thenReturn(Set.of("ONLINE_IMEI"));

        PageResponse<DeviceInfoDto> result = deviceService.listDevices(0, 20, null);

        assertTrue(result.getContent().get(0).getIsOnline());
    }

    /**
     * listDevices设备关联病人信息填充
     */
    @Test
    void listDevices_fillsPatientInfo() {
        Device d = new Device();
        d.setId(1L);
        d.setImei("IMEI001");
        Page<Device> page = new PageImpl<>(List.of(d));

        PatientDevice pd = new PatientDevice();
        pd.setDeviceId(1L);
        pd.setPatientId(10L);

        Patient patient = new Patient();
        patient.setId(10L);
        patient.setName("张三");

        when(deviceRepo.findAll(any(Pageable.class))).thenReturn(page);
        when(statusRepo.findAllById(anyList())).thenReturn(Collections.emptyList());
        when(patientDeviceRepo.findByDeviceIdIn(anyList())).thenReturn(List.of(pd));
        when(patientRepo.findAllById(anyList())).thenReturn(List.of(patient));
        when(downlinkManager.getOnlineImeis()).thenReturn(Collections.emptySet());

        PageResponse<DeviceInfoDto> result = deviceService.listDevices(0, 20, null);

        assertNotNull(result.getContent().get(0).getPatient());
        assertEquals("张三", result.getContent().get(0).getPatient().getName());
    }

    /**
     * 删除设备并清除缓存
     */
    @Test
    void deleteDevice_deletesAndClearsCache() {
        deviceService.deleteDevice(1L);
        verify(deviceRepo).deleteById(1L);
        verify(cacheService).delete(anyString());
    }

    /**
     * getDeviceById缓存命中返回数据
     */
    @Test
    void getDeviceById_cacheHit_returnsCachedData() {
        DeviceInfoDto cached = new DeviceInfoDto();
        cached.setId(1L);
        cached.setImei("IMEI001");
        when(cacheService.getOrSet(anyString(), eq(DeviceInfoDto.class), any(), anyLong(), any()))
                .thenReturn(cached);

        DeviceInfoDto result = deviceService.getDeviceById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("IMEI001", result.getImei());
    }

    /**
     * getDeviceByImei缓存命中返回数据
     */
    @Test
    void getDeviceByImei_cacheHit_returnsCachedData() {
        DeviceInfoDto cached = new DeviceInfoDto();
        cached.setId(1L);
        cached.setImei("IMEI001");
        when(cacheService.getOrSet(anyString(), eq(DeviceInfoDto.class), any(), anyLong(), any()))
                .thenReturn(cached);

        DeviceInfoDto result = deviceService.getDeviceByImei("IMEI001");

        assertNotNull(result);
        assertEquals("IMEI001", result.getImei());
    }

    /**
     * getAvailableDevices缓存命中返回数据
     */
    @Test
    void getAvailableDevices_cacheHit_returnsCachedData() {
        DeviceInfoDto dto = new DeviceInfoDto();
        dto.setId(1L);
        when(cacheService.get(anyString(), eq(List.class))).thenReturn(List.of(dto));

        List<DeviceInfoDto> result = deviceService.getAvailableDevices();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    /**
     * getAvailableDevices缓存未命中查询数据库
     */
    @Test
    void getAvailableDevices_cacheMiss_queriesDatabase() {
        when(cacheService.get(anyString(), eq(List.class))).thenReturn(null);
        Device d = new Device();
        d.setId(1L);
        d.setImei("IMEI001");
        when(deviceRepo.findAvailableDevices()).thenReturn(List.of(d));
        when(downlinkManager.getOnlineImeis()).thenReturn(Collections.emptySet());

        List<DeviceInfoDto> result = deviceService.getAvailableDevices();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("IMEI001", result.get(0).getImei());
        verify(cacheService).set(anyString(), any(), anyLong(), any());
    }
}
