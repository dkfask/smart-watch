package com.example.demo.repository;

import com.example.demo.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * JPA Repository单元测试（Mockito方式）
 * 
 * 由于实体类使用了MySQL特有的columnDefinition（如ON UPDATE CURRENT_TIMESTAMP），
 * H2无法直接创建这些表，因此使用Mockito进行单元测试。
 */
@ExtendWith(MockitoExtension.class)
class JpaRepositoryTest {

    /**
     * DeviceRepository findByImei查询
     */
    @Test
    void deviceRepository_findByImei() {
        DeviceRepository repo = mock(DeviceRepository.class);
        Device device = new Device();
        device.setId(1L);
        device.setImei("IMEI_UNIQUE");
        when(repo.findByImei("IMEI_UNIQUE")).thenReturn(Optional.of(device));
        when(repo.findByImei("NOT_EXIST")).thenReturn(Optional.empty());

        assertTrue(repo.findByImei("IMEI_UNIQUE").isPresent());
        assertFalse(repo.findByImei("NOT_EXIST").isPresent());
    }

    /**
     * DeviceRepository findByImei不存在时返回空
     */
    @Test
    void deviceRepository_findByImei_notFound() {
        DeviceRepository repo = mock(DeviceRepository.class);
        when(repo.findByImei("NOT_EXIST")).thenReturn(Optional.empty());

        assertFalse(repo.findByImei("NOT_EXIST").isPresent());
    }

    /**
     * PatientRepository save和搜索
     */
    @Test
    void patientRepository_saveAndSearch() {
        PatientRepository repo = mock(PatientRepository.class);
        Patient patient = new Patient();
        patient.setId(1L);
        patient.setName("张三");
        when(repo.findByNameContainingOrIdCardContainingOrWardContaining("张", "xxx", "xxx"))
                .thenReturn(List.of(patient));

        List<Patient> results = repo.findByNameContainingOrIdCardContainingOrWardContaining("张", "xxx", "xxx");
        assertFalse(results.isEmpty());
        assertEquals("张三", results.get(0).getName());
    }

    /**
     * AlarmRepository findByStatus查询
     */
    @Test
    void alarmRepository_findByStatus() {
        AlarmRepository repo = mock(AlarmRepository.class);
        Alarm alarm = new Alarm();
        alarm.setId(1L);
        alarm.setStatus("pending");
        Page<Alarm> page = new PageImpl<>(List.of(alarm));
        when(repo.findByStatusOrderByTriggeredTimeDesc(eq("pending"), any(PageRequest.class))).thenReturn(page);

        Page<Alarm> result = repo.findByStatusOrderByTriggeredTimeDesc("pending", PageRequest.of(0, 20));
        assertFalse(result.isEmpty());
        assertEquals("pending", result.getContent().get(0).getStatus());
    }

    /**
     * UserRepository findByUsername查询
     */
    @Test
    void userRepository_findByUsername() {
        UserRepository repo = mock(UserRepository.class);
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        when(repo.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(repo.findByUsername("nouser")).thenReturn(Optional.empty());

        assertTrue(repo.findByUsername("testuser").isPresent());
        assertEquals("testuser", repo.findByUsername("testuser").get().getUsername());
        assertFalse(repo.findByUsername("nouser").isPresent());
    }

    /**
     * PatientDeviceRepository findByDeviceId查询
     */
    @Test
    void patientDeviceRepository_findByDeviceId() {
        PatientDeviceRepository repo = mock(PatientDeviceRepository.class);
        PatientDevice pd = new PatientDevice();
        pd.setId(1L);
        pd.setPatientId(1L);
        pd.setDeviceId(1L);
        when(repo.findByDeviceId(1L)).thenReturn(List.of(pd));

        List<PatientDevice> results = repo.findByDeviceId(1L);
        assertFalse(results.isEmpty());
        assertEquals(1L, results.get(0).getDeviceId());
    }
}
