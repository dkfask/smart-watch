package com.example.demo.controller.api;

import com.example.demo.model.PatientDevice;
import com.example.demo.repository.PatientDeviceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientByDeviceController {
    private final PatientDeviceRepository patientDeviceRepo;
    private final PatientController patientController;

    public PatientByDeviceController(PatientDeviceRepository patientDeviceRepo, PatientController patientController) {
        this.patientDeviceRepo = patientDeviceRepo;
        this.patientController = patientController;
    }

    /**
     * 根据设备ID获取病人信息
     */
    @GetMapping("/by-device/{deviceId}")
    public ResponseEntity<?> getPatientByDeviceId(@PathVariable long deviceId) {
        List<PatientDevice> patientDevices = patientDeviceRepo.findByDeviceId(deviceId);
        if (patientDevices.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return patientController.get(patientDevices.get(0).getPatientId());
    }
}
