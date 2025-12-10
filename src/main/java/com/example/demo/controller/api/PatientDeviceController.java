package com.example.demo.controller.api;

import com.example.demo.model.PatientDevice;
import com.example.demo.repository.PatientDeviceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patient-devices")
public class PatientDeviceController {
    private final PatientDeviceRepository repo;

    public PatientDeviceController(PatientDeviceRepository repo) { this.repo = repo; }

    public static class BindRequest {
        public Long patientId;
        public Long deviceId;
        public String relationship = "wearing"; // 默认值
    }

    // 关联设备和病人
    @PostMapping
    public ResponseEntity<?> bind(@RequestBody BindRequest req) {
        // 检查是否已存在关联
        PatientDevice existing = repo.findByPatientIdAndDeviceId(req.patientId, req.deviceId);
        if (existing != null) {
            // 更新现有关联
            existing.setRelationship(req.relationship);
            existing.setIsActive(true);
            repo.save(existing);
            return ResponseEntity.ok(existing);
        }
        
        // 创建新关联
        PatientDevice pd = new PatientDevice();
        pd.setPatientId(req.patientId);
        pd.setDeviceId(req.deviceId);
        pd.setRelationship(req.relationship);
        pd = repo.save(pd);
        return ResponseEntity.ok(pd);
    }

    // 解除关联
    @DeleteMapping
    public ResponseEntity<?> unbind(@RequestParam long patientId, @RequestParam long deviceId) {
        int deleted = repo.unbindDeviceFromPatient(patientId, deviceId);
        return deleted > 0 ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    // 获取病人关联设备
    @GetMapping("/by-patient/{patientId}")
    public List<PatientDevice> byPatient(@PathVariable long patientId) {
        return repo.findByPatientId(patientId);
    }

    // 获取设备关联病人
    @GetMapping("/by-device/{deviceId}")
    public List<PatientDevice> byDevice(@PathVariable long deviceId) {
        return repo.findByDeviceId(deviceId);
    }
}

// 新增：根据设备ID获取病人信息的API
@RestController
@RequestMapping("/api/patients")
class PatientByDeviceController {
    private final PatientDeviceRepository patientDeviceRepo;
    private final PatientController patientController;

    public PatientByDeviceController(PatientDeviceRepository patientDeviceRepo, PatientController patientController) {
        this.patientDeviceRepo = patientDeviceRepo;
        this.patientController = patientController;
    }

    @GetMapping("/by-device/{deviceId}")
    public ResponseEntity<?> getPatientByDeviceId(@PathVariable long deviceId) {
        List<PatientDevice> patientDevices = patientDeviceRepo.findByDeviceId(deviceId);
        if (patientDevices.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        // 返回第一个关联的病人
        return patientController.get(patientDevices.get(0).getPatientId());
    }
}
