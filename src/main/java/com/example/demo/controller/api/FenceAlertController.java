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
        // JpaRepository的findByDeviceId方法，无需分页参数
        return repo.findByDeviceId(deviceId);
    }

    @PutMapping("/{alertId}/read")
    public ResponseEntity<?> markRead(@PathVariable long alertId, @RequestParam(defaultValue = "true") boolean read) {
        // JpaRepository的findById方法
        return repo.findById(alertId)
                .map(alert -> {
                    // 更新报警状态为已处理
                    alert.setStatus(read ? "handled" : "pending");
                    repo.save(alert);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}

