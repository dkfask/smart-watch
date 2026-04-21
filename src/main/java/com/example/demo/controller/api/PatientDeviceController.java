package com.example.demo.controller.api;

import com.example.demo.model.PatientDevice;
import com.example.demo.repository.PatientDeviceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
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
        public String relationship = "wearing";
    }

    /**
     * 关联设备和病人（一对一关系）
     */
    @PostMapping
    public ResponseEntity<?> bind(@RequestBody BindRequest req) {
        PatientDevice existing = repo.findByPatientIdAndDeviceId(req.patientId, req.deviceId);
        if (existing != null) {
            existing.setRelationship(req.relationship);
            existing.setIsActive(true);
            repo.save(existing);
            return ResponseEntity.ok(existing);
        }

        List<PatientDevice> patientExisting = repo.findByPatientId(req.patientId);
        for (PatientDevice pd : patientExisting) {
            pd.setIsActive(false);
            repo.save(pd);
        }

        List<PatientDevice> deviceExisting = repo.findByDeviceId(req.deviceId);
        for (PatientDevice pd : deviceExisting) {
            pd.setIsActive(false);
            repo.save(pd);
        }

        PatientDevice pd = new PatientDevice();
        pd.setPatientId(req.patientId);
        pd.setDeviceId(req.deviceId);
        pd.setRelationship(req.relationship);
        pd = repo.save(pd);
        return ResponseEntity.ok(pd);
    }

    /**
     * 解除关联
     */
    @DeleteMapping
    @Transactional
    public ResponseEntity<?> unbind(@RequestParam long patientId, @RequestParam long deviceId) {
        int deleted = repo.unbindDeviceFromPatient(patientId, deviceId);
        return deleted > 0 ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /**
     * 获取病人关联设备
     */
    @GetMapping("/by-patient/{patientId}")
    public List<PatientDevice> byPatient(@PathVariable long patientId) {
        return repo.findByPatientId(patientId);
    }

    /**
     * 获取设备关联病人
     */
    @GetMapping("/by-device/{deviceId}")
    public List<PatientDevice> byDevice(@PathVariable long deviceId) {
        return repo.findByDeviceId(deviceId);
    }
}
