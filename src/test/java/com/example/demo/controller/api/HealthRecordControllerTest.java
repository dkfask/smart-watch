package com.example.demo.controller.api;

import com.example.demo.model.HealthRecord;
import com.example.demo.model.PatientDevice;
import com.example.demo.repository.HealthRecordRepository;
import com.example.demo.repository.PatientDeviceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = HealthRecordController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
class HealthRecordControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private HealthRecordRepository repo;
    @MockBean private PatientDeviceRepository patientDeviceRepo;
    @MockBean private RateLimiter rateLimiter;

    /**
     * 构建测试用的HealthRecord对象
     */
    private HealthRecord buildRecord(Long id, Long patientId, String imei, String dataType, String value, Date recvTime) {
        HealthRecord r = new HealthRecord();
        r.setId(id);
        r.setPatientId(patientId);
        r.setImei(imei);
        r.setDataType(dataType);
        r.setValue(value);
        r.setRecvTime(recvTime);
        r.setCreatedAt(recvTime);
        r.setUpdatedAt(recvTime);
        return r;
    }

    private PatientDevice buildBinding(Long patientId, Long deviceId) {
        PatientDevice binding = new PatientDevice();
        binding.setPatientId(patientId);
        binding.setDeviceId(deviceId);
        binding.setIsActive(true);
        return binding;
    }

    /**
     * GET /api/health-records 按patientId查询
     */
    @Test
    void list_byPatientId_returnsRecords() throws Exception {
        Date now = new Date();
        List<HealthRecord> records = List.of(
                buildRecord(1L, 100L, null, "temperature", "36.5", now)
        );
        when(repo.findByPatientIdOrderByRecvTimeDesc(100L)).thenReturn(records);

        mockMvc.perform(get("/api/health-records")
                        .param("patientId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    /**
     * GET /api/health-records 按patientId+dataType查询
     */
    @Test
    void list_byPatientIdAndDataType_returnsRecords() throws Exception {
        Date now = new Date();
        List<HealthRecord> records = List.of(
                buildRecord(1L, 100L, null, "temperature", "36.5", now)
        );
        when(repo.findByPatientIdAndDataTypeOrderByRecvTimeDesc(100L, "temperature")).thenReturn(records);

        mockMvc.perform(get("/api/health-records")
                        .param("patientId", "100")
                        .param("dataType", "temperature"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    /**
     * GET /api/health-records 按imei查询
     */
    @Test
    void list_byImei_returnsRecords() throws Exception {
        Date now = new Date();
        List<HealthRecord> records = List.of(
                buildRecord(1L, 100L, "1234567890", "heart_rate", "72", now)
        );
        when(repo.findByImeiOrderByRecvTimeDesc("1234567890")).thenReturn(records);

        mockMvc.perform(get("/api/health-records")
                        .param("imei", "1234567890"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    /**
     * GET /api/health-records 无参数全量查询
     */
    @Test
    void list_noParams_returnsAllRecords() throws Exception {
        Date now = new Date();
        List<HealthRecord> all = List.of(
                buildRecord(1L, 100L, null, "temperature", "36.5", now),
                buildRecord(2L, 101L, null, "heart_rate", "80", now)
        );
        when(repo.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(all));

        mockMvc.perform(get("/api/health-records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.total").value(2));
    }

    /**
     * GET /api/health-records 分页offset/limit正常工作
     */
    @Test
    void list_withPagination_returnsSlicedRecords() throws Exception {
        Date now = new Date();
        List<HealthRecord> all = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            all.add(buildRecord((long) i, 100L, null, "temperature", String.valueOf(36 + i * 0.1), now));
        }
        when(repo.findByPatientIdOrderByRecvTimeDesc(100L)).thenReturn(all);

        mockMvc.perform(get("/api/health-records")
                        .param("patientId", "100")
                        .param("offset", "2")
                        .param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.total").value(10));
    }

    /**
     * GET /api/health-records/latest 返回各类健康数据的最新值
     */
    @Test
    void latest_returnsLatestHealthData() throws Exception {
        Date now = new Date();
        when(repo.findTopByPatientIdAndDataTypeOrderByRecvTimeDesc(100L, "temperature"))
                .thenReturn(Optional.of(buildRecord(1L, 100L, null, "temperature", "36.5", now)));
        when(repo.findTopByPatientIdAndDataTypeOrderByRecvTimeDesc(100L, "heart_rate"))
                .thenReturn(Optional.of(buildRecord(2L, 100L, null, "heart_rate", "72", now)));
        when(repo.findTopByPatientIdAndDataTypeOrderByRecvTimeDesc(100L, "blood_pressure"))
                .thenReturn(Optional.of(buildRecord(3L, 100L, null, "blood_pressure", "120/80", now)));
        when(repo.findTopByPatientIdAndDataTypeOrderByRecvTimeDesc(100L, "spo2"))
                .thenReturn(Optional.of(buildRecord(4L, 100L, null, "spo2", "98", now)));
        when(repo.findTopByPatientIdAndDataTypeOrderByRecvTimeDesc(100L, "blood_oxygen"))
                .thenReturn(Optional.of(buildRecord(5L, 100L, null, "blood_oxygen", "97", now)));

        mockMvc.perform(get("/api/health-records/latest")
                        .param("patientId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.temperature.value").value("36.5"))
                .andExpect(jsonPath("$.data.temperature.unit").value("°C"))
                .andExpect(jsonPath("$.data.heart_rate.value").value("72"))
                .andExpect(jsonPath("$.data.heart_rate.unit").value("次/分"))
                .andExpect(jsonPath("$.data.blood_pressure.value").value("120/80"))
                .andExpect(jsonPath("$.data.blood_pressure.unit").value("mmHg"))
                .andExpect(jsonPath("$.data.spo2.value").value("98"))
                .andExpect(jsonPath("$.data.spo2.unit").value("%"))
                .andExpect(jsonPath("$.data.blood_oxygen.value").value("97"))
                .andExpect(jsonPath("$.data.blood_oxygen.unit").value("%"));
    }

    /**
     * GET /api/health-records/latest 无数据时返回空Map
     */
    @Test
    void latest_noData_returnsEmptyMap() throws Exception {
        when(repo.findTopByPatientIdAndDataTypeOrderByRecvTimeDesc(eq(100L), anyString()))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/api/health-records/latest")
                        .param("patientId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void latest_mapsProtocolTypeAliasesForFrontend() throws Exception {
        Date now = new Date();
        when(repo.findTopByPatientIdAndDataTypeOrderByRecvTimeDesc(100L, "temperature"))
                .thenReturn(Optional.empty());
        when(repo.findTopByPatientIdAndDataTypeOrderByRecvTimeDesc(100L, "body_temperature"))
                .thenReturn(Optional.of(buildRecord(1L, 100L, null, "body_temperature", "36.8", now)));
        when(repo.findTopByPatientIdAndDataTypeOrderByRecvTimeDesc(100L, "spo2"))
                .thenReturn(Optional.empty());
        when(repo.findTopByPatientIdAndDataTypeOrderByRecvTimeDesc(100L, "blood_oxygen"))
                .thenReturn(Optional.of(buildRecord(2L, 100L, null, "blood_oxygen", "97", now)));

        mockMvc.perform(get("/api/health-records/latest")
                        .param("patientId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.temperature.value").value("36.8"))
                .andExpect(jsonPath("$.data.temperature.unit").value("°C"))
                .andExpect(jsonPath("$.data.spo2.value").value("97"))
                .andExpect(jsonPath("$.data.spo2.unit").value("%"));
    }

    @Test
    void latest_fallsBackToBoundDeviceRecords_whenPatientIdWasNotSaved() throws Exception {
        Date now = new Date();
        HealthRecord record = buildRecord(1L, null, "359999000000001", "heart_rate", "88", now);
        record.setDeviceId(15L);

        when(patientDeviceRepo.findByPatientId(100L)).thenReturn(List.of(buildBinding(100L, 15L)));
        when(repo.findTopByPatientIdAndDataTypeOrderByRecvTimeDesc(eq(100L), anyString()))
                .thenReturn(Optional.empty());
        when(repo.findByDeviceIdInAndDataTypeOrderByRecvTimeDesc(List.of(15L), "heart_rate"))
                .thenReturn(List.of(record));

        mockMvc.perform(get("/api/health-records/latest")
                        .param("patientId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.heart_rate.value").value("88"));
    }

    /**
     * GET /api/health-records/stats 带patientId+dataType+时间范围查询
     */
    @Test
    void stats_withTimeRange_returnsStats() throws Exception {
        Date now = new Date();
        List<HealthRecord> records = List.of(
                buildRecord(1L, 100L, null, "temperature", "36.5", now),
                buildRecord(2L, 100L, null, "temperature", "37.0", now),
                buildRecord(3L, 100L, null, "temperature", "37.5", now)
        );
        when(repo.findByPatientIdAndRecvTimeBetweenOrderByRecvTimeDesc(eq(100L), any(Date.class), any(Date.class)))
                .thenReturn(records);

        long start = System.currentTimeMillis() - 86400000L;
        long end = System.currentTimeMillis();

        mockMvc.perform(get("/api/health-records/stats")
                        .param("patientId", "100")
                        .param("dataType", "temperature")
                        .param("start", String.valueOf(start))
                        .param("end", String.valueOf(end)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRecords").value(3))
                .andExpect(jsonPath("$.data.dataType").value("temperature"))
                .andExpect(jsonPath("$.data.min").value(36.5))
                .andExpect(jsonPath("$.data.max").value(37.5))
                .andExpect(jsonPath("$.data.avg").value(37.0))
                .andExpect(jsonPath("$.data.count").value(3));
    }

    /**
     * GET /api/health-records/stats 带patientId+dataType（无时间范围）
     */
    @Test
    void stats_withDataType_noTimeRange_returnsStats() throws Exception {
        Date now = new Date();
        List<HealthRecord> records = List.of(
                buildRecord(1L, 100L, null, "heart_rate", "70", now),
                buildRecord(2L, 100L, null, "heart_rate", "80", now)
        );
        when(repo.findByPatientIdAndDataTypeOrderByRecvTimeDesc(100L, "heart_rate")).thenReturn(records);

        mockMvc.perform(get("/api/health-records/stats")
                        .param("patientId", "100")
                        .param("dataType", "heart_rate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRecords").value(2))
                .andExpect(jsonPath("$.data.dataType").value("heart_rate"))
                .andExpect(jsonPath("$.data.min").value(70.0))
                .andExpect(jsonPath("$.data.max").value(80.0))
                .andExpect(jsonPath("$.data.count").value(2));
    }

    /**
     * GET /api/health-records/stats 仅patientId查询
     */
    @Test
    void stats_patientIdOnly_returnsTotalRecords() throws Exception {
        Date now = new Date();
        List<HealthRecord> records = List.of(
                buildRecord(1L, 100L, null, "temperature", "36.5", now),
                buildRecord(2L, 100L, null, "heart_rate", "72", now)
        );
        when(repo.findByPatientIdOrderByRecvTimeDesc(100L)).thenReturn(records);

        mockMvc.perform(get("/api/health-records/stats")
                        .param("patientId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRecords").value(2));
    }

    /**
     * GET /api/health-records/stats 有dataType时返回min/max/avg/count统计
     */
    @Test
    void stats_withDataType_returnsMinMaxAvgCount() throws Exception {
        Date now = new Date();
        List<HealthRecord> records = List.of(
                buildRecord(1L, 100L, null, "spo2", "95", now),
                buildRecord(2L, 100L, null, "spo2", "97", now),
                buildRecord(3L, 100L, null, "spo2", "99", now)
        );
        when(repo.findByPatientIdAndDataTypeOrderByRecvTimeDesc(100L, "spo2")).thenReturn(records);

        mockMvc.perform(get("/api/health-records/stats")
                        .param("patientId", "100")
                        .param("dataType", "spo2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRecords").value(3))
                .andExpect(jsonPath("$.data.dataType").value("spo2"))
                .andExpect(jsonPath("$.data.min").value(95.0))
                .andExpect(jsonPath("$.data.max").value(99.0))
                .andExpect(jsonPath("$.data.avg").value(97.0))
                .andExpect(jsonPath("$.data.count").value(3));
    }

    /**
     * POST /api/health-records 创建成功返回200和id
     */
    @Test
    void create_success_returns200WithId() throws Exception {
        HealthRecord saved = buildRecord(1L, 100L, null, "temperature", "36.5", new Date());
        when(repo.save(any(HealthRecord.class))).thenReturn(saved);

        Map<String, Object> body = new HashMap<>();
        body.put("patientId", 100);
        body.put("dataType", "temperature");
        body.put("value", "36.5");

        mockMvc.perform(post("/api/health-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    /**
     * POST /api/health-records 自动填充createdAt/updatedAt/recvTime（如果为null）
     */
    @Test
    void create_autoFillsTimestamps_whenNull() throws Exception {
        HealthRecord saved = buildRecord(2L, 100L, null, "heart_rate", "72", new Date());
        when(repo.save(any(HealthRecord.class))).thenReturn(saved);

        Map<String, Object> body = new HashMap<>();
        body.put("patientId", 100);
        body.put("dataType", "heart_rate");
        body.put("value", "72");

        mockMvc.perform(post("/api/health-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(2));
    }
}
