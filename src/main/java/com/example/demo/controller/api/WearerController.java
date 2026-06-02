package com.example.demo.controller.api;

import com.example.demo.model.dto.PageResponse;
import com.example.demo.model.dto.WearerDto;
import com.example.demo.service.WearerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/wearers")
public class WearerController {

    private final WearerService wearerService;

    public WearerController(WearerService wearerService) {
        this.wearerService = wearerService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<WearerDto>> getWearers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset) {
        int effectivePage = (offset != null && size > 0) ? offset / size : page;
        int effectiveSize = limit != null ? limit : size;
        return ResponseEntity.ok(wearerService.getWearers(effectivePage, effectiveSize));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WearerDto> getWearer(@PathVariable Long id) {
        return ResponseEntity.ok(wearerService.getWearer(id));
    }

    @PostMapping
    public ResponseEntity<WearerDto> createWearer(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(wearerService.createWearer(body));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WearerDto> updateWearer(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(wearerService.updateWearer(id, body));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWearer(@PathVariable Long id) {
        wearerService.deleteWearer(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/assign-device")
    public ResponseEntity<WearerDto> assignDevice(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long deviceId = Long.valueOf(body.get("deviceId").toString());
        return ResponseEntity.ok(wearerService.assignDevice(id, deviceId));
    }

    @PostMapping("/{id}/unassign-device")
    public ResponseEntity<WearerDto> unassignDevice(@PathVariable Long id) {
        return ResponseEntity.ok(wearerService.unassignDevice(id));
    }

    @GetMapping("/by-device/{deviceId}")
    public ResponseEntity<WearerDto> getWearerByDeviceId(@PathVariable Long deviceId) {
        return ResponseEntity.ok(wearerService.getWearerByDeviceId(deviceId));
    }
}
