package com.example.demo.controller.api;

import com.example.demo.model.UserDevice;
import com.example.demo.repository.UserDeviceRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/user-devices")
public class UserDeviceController {
    private final UserDeviceRepository repo;

    public UserDeviceController(UserDeviceRepository repo) { this.repo = repo; }

    public static class BindRequest {
        public Long userId;
        public Long deviceId;
        public String relationship; // owner/viewer/guardian
    }

    @PostMapping
    public ResponseEntity<?> bind(@RequestBody BindRequest req) {
        long id = repo.bind(req.userId, req.deviceId, req.relationship == null ? "owner" : req.relationship);
        return ResponseEntity.created(URI.create("/api/user-devices/" + id)).body(id);
    }

    @DeleteMapping
    public ResponseEntity<?> unbind(@RequestParam long userId, @RequestParam long deviceId) {
        int n = repo.unbind(userId, deviceId);
        return n > 0 ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/by-user/{userId}")
    public List<UserDevice> byUser(@PathVariable long userId) {
        return repo.findByUser(userId);
    }

    @GetMapping("/by-device/{deviceId}")
    public List<UserDevice> byDevice(@PathVariable long deviceId) {
        return repo.findByDevice(deviceId);
    }
}

