package com.example.demo.controller.api;

import com.example.demo.model.FenceAlert;
import com.example.demo.repository.FenceAlertRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class FenceAlertController {
    private final FenceAlertRepository repo;

    public FenceAlertController(FenceAlertRepository repo) { this.repo = repo; }

    @GetMapping("/by-device/{deviceId}")
    public List<FenceAlert> byDevice(@PathVariable long deviceId,
                                     @RequestParam(defaultValue = "50") int limit,
                                     @RequestParam(defaultValue = "0") int offset) {
        return repo.listByDevice(deviceId, limit, offset);
    }

    @PutMapping("/{alertId}/read")
    public ResponseEntity<?> markRead(@PathVariable long alertId, @RequestParam(defaultValue = "true") boolean read) {
        int n = repo.markRead(alertId, read);
        return n > 0 ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
}

