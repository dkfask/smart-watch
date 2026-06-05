package com.example.demo.controller.api;

import com.example.demo.model.HealthRecord;
import com.example.demo.model.dto.PageResponse;
import com.example.demo.repository.HealthRecordRepository;
import com.example.demo.repository.PatientDeviceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 健康数据API控制器
 */
@RestController
@RequestMapping("/api/health-records")
public class HealthRecordController {

    private final HealthRecordRepository repo;
    private final PatientDeviceRepository patientDeviceRepo;

    public HealthRecordController(HealthRecordRepository repo, PatientDeviceRepository patientDeviceRepo) {
        this.repo = repo;
        this.patientDeviceRepo = patientDeviceRepo;
    }

    /**
     * 获取健康记录列表（分页）
     */
    @GetMapping
    public ResponseEntity<?> getHealthRecords(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) String imei,
            @RequestParam(required = false) String dataType,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        int effectivePage = (page != null) ? page : (offset / Math.max(limit, 1));
        int effectiveSize = (size != null) ? size : limit;

        if (patientId != null) {
            if (dataType != null) {
                var pageable = PageRequest.of(effectivePage, effectiveSize, Sort.by(Sort.Direction.DESC, "recvTime"));
                List<HealthRecord> records = findRecordsByPatientAndType(patientId, dataType);
                return ResponseEntity.ok(PageResponse.from(records, records.size(), pageable));
            } else {
                var pageable = PageRequest.of(effectivePage, effectiveSize, Sort.by(Sort.Direction.DESC, "recvTime"));
                List<HealthRecord> records = findRecordsByPatient(patientId);
                return ResponseEntity.ok(PageResponse.from(records, records.size(), pageable));
            }
        } else if (imei != null) {
            var pageable = PageRequest.of(effectivePage, effectiveSize, Sort.by(Sort.Direction.DESC, "recvTime"));
            List<HealthRecord> records = repo.findByImeiOrderByRecvTimeDesc(imei);
            return ResponseEntity.ok(PageResponse.from(records, records.size(), pageable));
        } else {
            var pageable = PageRequest.of(effectivePage, effectiveSize, Sort.by(Sort.Direction.DESC, "createdAt"));
            return ResponseEntity.ok(PageResponse.from(repo.findAll(pageable)));
        }
    }

    /**
     * 根据设备ID获取健康记录（分页）
     */
    @GetMapping("/device/{deviceId}")
    public ResponseEntity<PageResponse<HealthRecord>> getHealthRecordsByDevice(
            @PathVariable Long deviceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "recvTime"));
        return ResponseEntity.ok(PageResponse.from(repo.findByDeviceId(deviceId, pageable)));
    }

    /**
     * 获取病人最新的各类健康数据
     */
    @GetMapping("/latest")
    public ResponseEntity<?> getLatestHealthRecords(@RequestParam Long patientId) {
        Map<String, Object> latest = new HashMap<>();
        Map<String, List<String>> typeAliases = new LinkedHashMap<>();
        typeAliases.put("temperature", List.of("temperature", "body_temperature"));
        typeAliases.put("heart_rate", List.of("heart_rate"));
        typeAliases.put("blood_pressure", List.of("blood_pressure"));
        typeAliases.put("spo2", List.of("spo2", "blood_oxygen"));

        for (Map.Entry<String, List<String>> alias : typeAliases.entrySet()) {
            Optional<HealthRecord> record = Optional.empty();
            for (String dataType : alias.getValue()) {
                record = findLatestByPatientAndType(patientId, dataType);
                if (record.isPresent()) {
                    break;
                }
            }
            if (record.isPresent()) {
                HealthRecord r = record.get();
                Map<String, Object> entry = buildLatestEntry(r, alias.getKey());
                latest.put(alias.getKey(), entry);
            }
        }
        putLegacyLatestEntry(latest, patientId, "body_temperature", "temperature");
        putLegacyLatestEntry(latest, patientId, "blood_oxygen", "spo2");
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
     * 更新健康记录
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateHealthRecord(@PathVariable Long id, @RequestBody HealthRecord record) {
        HealthRecord existing = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Health record not found: " + id));
        existing.setPatientId(record.getPatientId() != null ? record.getPatientId() : existing.getPatientId());
        existing.setImei(record.getImei() != null ? record.getImei() : existing.getImei());
        existing.setDataType(record.getDataType() != null ? record.getDataType() : existing.getDataType());
        existing.setValue(record.getValue() != null ? record.getValue() : existing.getValue());
        existing.setRecvTime(record.getRecvTime() != null ? record.getRecvTime() : existing.getRecvTime());
        existing.setUpdatedAt(new Date());
        repo.save(existing);
        return ResponseEntity.ok(Map.of("id", existing.getId()));
    }

    /**
     * 删除健康记录
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteHealthRecord(@PathVariable Long id) {
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Health record not found: " + id);
        }
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private String getUnitForType(String dataType) {
        return switch (dataType) {
            case "temperature", "body_temperature" -> "°C";
            case "heart_rate" -> "次/分";
            case "blood_pressure" -> "mmHg";
            case "spo2", "blood_oxygen" -> "%";
            default -> "";
        };
    }

    private Map<String, Object> buildLatestEntry(HealthRecord record, String dataType) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("value", record.getValue());
        entry.put("time", record.getRecvTime());
        entry.put("unit", getUnitForType(dataType));
        return entry;
    }

    private void putLegacyLatestEntry(Map<String, Object> latest, Long patientId, String legacyType, String fallbackKey) {
        Optional<HealthRecord> legacyRecord = findLatestByPatientAndType(patientId, legacyType);
        if (legacyRecord.isPresent()) {
            latest.put(legacyType, buildLatestEntry(legacyRecord.get(), legacyType));
        } else if (latest.containsKey(fallbackKey)) {
            latest.put(legacyType, latest.get(fallbackKey));
        }
    }

    private List<Long> getBoundDeviceIds(Long patientId) {
        List<com.example.demo.model.PatientDevice> bindings = patientDeviceRepo.findByPatientId(patientId);
        if (bindings == null || bindings.isEmpty()) {
            return List.of();
        }
        return bindings.stream()
                .filter(pd -> pd.getIsActive() == null || pd.getIsActive())
                .map(pd -> pd.getDeviceId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private List<HealthRecord> findRecordsByPatient(Long patientId) {
        List<HealthRecord> records = new ArrayList<>(repo.findByPatientIdOrderByRecvTimeDesc(patientId));
        List<Long> deviceIds = getBoundDeviceIds(patientId);
        if (!deviceIds.isEmpty()) {
            records.addAll(repo.findByDeviceIdInOrderByRecvTimeDesc(deviceIds).stream()
                    .filter(r -> r.getPatientId() == null || Objects.equals(r.getPatientId(), patientId))
                    .toList());
        }
        return records.stream()
                .sorted(Comparator.comparing(HealthRecord::getRecvTime, Comparator.nullsLast(Date::compareTo)).reversed())
                .toList();
    }

    private List<HealthRecord> findRecordsByPatientAndType(Long patientId, String dataType) {
        List<HealthRecord> records = new ArrayList<>(repo.findByPatientIdAndDataTypeOrderByRecvTimeDesc(patientId, dataType));
        List<Long> deviceIds = getBoundDeviceIds(patientId);
        if (!deviceIds.isEmpty()) {
            records.addAll(repo.findByDeviceIdInAndDataTypeOrderByRecvTimeDesc(deviceIds, dataType).stream()
                    .filter(r -> r.getPatientId() == null || Objects.equals(r.getPatientId(), patientId))
                    .toList());
        }
        return records.stream()
                .sorted(Comparator.comparing(HealthRecord::getRecvTime, Comparator.nullsLast(Date::compareTo)).reversed())
                .toList();
    }

    private Optional<HealthRecord> findLatestByPatientAndType(Long patientId, String dataType) {
        Optional<HealthRecord> byPatient = repo.findTopByPatientIdAndDataTypeOrderByRecvTimeDesc(patientId, dataType);
        List<Long> deviceIds = getBoundDeviceIds(patientId);
        Optional<HealthRecord> byDevice = Optional.empty();
        if (!deviceIds.isEmpty()) {
            byDevice = repo.findByDeviceIdInAndDataTypeOrderByRecvTimeDesc(deviceIds, dataType).stream()
                    .filter(r -> r.getPatientId() == null || Objects.equals(r.getPatientId(), patientId))
                    .findFirst();
        }
        if (byPatient.isEmpty()) {
            return byDevice;
        }
        if (byDevice.isEmpty()) {
            return byPatient;
        }
        Date patientTime = byPatient.get().getRecvTime();
        Date deviceTime = byDevice.get().getRecvTime();
        if (patientTime == null) return byDevice;
        if (deviceTime == null) return byPatient;
        return deviceTime.after(patientTime) ? byDevice : byPatient;
    }

    private Double parseDoubleSafe(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}
