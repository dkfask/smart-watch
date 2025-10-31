package com.example.demo.controller.api;

import com.example.demo.model.DeviceStatus;
import com.example.demo.repository.DeviceStatusRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/status")
public class DeviceStatusController {
    private final DeviceStatusRepository repo;

    public DeviceStatusController(DeviceStatusRepository repo) { this.repo = repo; }

    @GetMapping("/{deviceId}")
    public ResponseEntity<?> get(@PathVariable long deviceId) {
        Optional<DeviceStatus> s = repo.findById(deviceId);
        return s.<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{deviceId}/online")
    public ResponseEntity<?> setOnline(@PathVariable long deviceId, @RequestParam boolean online) {
        int n = repo.setOnline(deviceId, online);
        return n > 0 ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
}

