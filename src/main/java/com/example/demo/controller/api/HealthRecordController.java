package com.example.demo.controller.api;

import com.example.demo.model.HealthRecord;
import com.example.demo.repository.HealthRecordRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 健康数据API控制器
 */
@RestController
@RequestMapping("/api/health-records")
public class HealthRecordController {

    private final HealthRecordRepository repo;

    public HealthRecordController(HealthRecordRepository repo) {
        this.repo = repo;
    }

    /**
     * 获取病人的健康记录列表
     */
    @GetMapping
    public ResponseEntity<?> getHealthRecords(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) String imei,
            @RequestParam(required = false) String dataType,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        List<HealthRecord> records;
        if (patientId != null) {
            if (dataType != null) {
                records = repo.findByPatientIdAndDataTypeOrderByRecvTimeDesc(patientId, dataType);
            } else {
                records = repo.findByPatientIdOrderByRecvTimeDesc(patientId);
            }
        } else if (imei != null) {
            records = repo.findByImeiOrderByRecvTimeDesc(imei);
        } else {
            List<HealthRecord> all = new ArrayList<>();
            repo.findAll().forEach(all::add);
            records = all;
        }
        int end = Math.min(offset + limit, records.size());
        if (offset >= records.size()) {
            return ResponseEntity.ok(Map.of("content", Collections.emptyList(), "totalElements", records.size()));
        }
        return ResponseEntity.ok(Map.of("content", records.subList(offset, end), "totalElements", records.size()));
    }

    /**
     * 获取病人最新的各类健康数据
     */
    @GetMapping("/latest")
    public ResponseEntity<?> getLatestHealthRecords(@RequestParam Long patientId) {
        Map<String, Object> latest = new HashMap<>();
        String[] types = {"temperature", "heart_rate", "blood_pressure", "spo2", "blood_oxygen"};
        for (String type : types) {
            Optional<HealthRecord> record = repo.findTopByPatientIdAndDataTypeOrderByRecvTimeDesc(patientId, type);
            if (record.isPresent()) {
                HealthRecord r = record.get();
                Map<String, Object> entry = new HashMap<>();
                entry.put("value", r.getValue());
                entry.put("time", r.getRecvTime());
                entry.put("unit", getUnitForType(type));
                latest.put(type, entry);
            }
        }
        return ResponseEntity.ok(latest);
    }

    /**
     * 获取病人健康数据统计
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getHealthStats(
            @RequestParam Long patientId,
            @RequestParam(required = false) String dataType,
            @RequestParam(required = false) Long start,
            @RequestParam(required = false) Long end) {
        List<HealthRecord> records;
        if (start != null && end != null) {
            records = repo.findByPatientIdAndRecvTimeBetweenOrderByRecvTimeDesc(
                    patientId, new Date(start), new Date(end));
        } else if (dataType != null) {
            records = repo.findByPatientIdAndDataTypeOrderByRecvTimeDesc(patientId, dataType);
        } else {
            records = repo.findByPatientIdOrderByRecvTimeDesc(patientId);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRecords", records.size());
        if (dataType != null && !records.isEmpty()) {
            List<Double> values = records.stream()
                    .map(r -> parseDoubleSafe(r.getValue()))
                    .filter(v -> !v.isNaN())
                    .toList();
            if (!values.isEmpty()) {
                stats.put("dataType", dataType);
                stats.put("min", values.stream().mapToDouble(Double::doubleValue).min().orElse(0));
                stats.put("max", values.stream().mapToDouble(Double::doubleValue).max().orElse(0));
                stats.put("avg", values.stream().mapToDouble(Double::doubleValue).average().orElse(0));
                stats.put("count", values.size());
            }
        }
        return ResponseEntity.ok(stats);
    }

    /**
     * 创建健康记录
     */
    @PostMapping
    public ResponseEntity<?> createHealthRecord(@RequestBody HealthRecord record) {
        if (record.getCreatedAt() == null) record.setCreatedAt(new Date());
        if (record.getUpdatedAt() == null) record.setUpdatedAt(new Date());
        if (record.getRecvTime() == null) record.setRecvTime(new Date());
        HealthRecord saved = repo.save(record);
        return ResponseEntity.ok(Map.of("id", saved.getId()));
    }

    /**
     * 根据数据类型获取单位
     */
    private String getUnitForType(String dataType) {
        return switch (dataType) {
            case "temperature" -> "°C";
            case "heart_rate" -> "次/分";
            case "blood_pressure" -> "mmHg";
            case "spo2", "blood_oxygen" -> "%";
            default -> "";
        };
    }

    /**
     * 安全解析double值
     */
    private Double parseDoubleSafe(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}
