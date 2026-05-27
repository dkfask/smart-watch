package com.example.demo.controller.api;

import com.example.demo.model.HealthAlertThreshold;
import com.example.demo.model.dto.HealthAlertThresholdDto;
import com.example.demo.repository.HealthAlertThresholdRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/health-alert-thresholds")
public class HealthAlertThresholdController {

    private final HealthAlertThresholdRepository repo;

    public HealthAlertThresholdController(HealthAlertThresholdRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public ResponseEntity<List<HealthAlertThresholdDto>> getThresholds() {
        List<HealthAlertThresholdDto> dtos = repo.findAll().stream()
                .map(HealthAlertThresholdDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PutMapping
    public ResponseEntity<List<HealthAlertThresholdDto>> updateThresholds(
            @RequestBody List<Map<String, Object>> thresholds) {
        List<HealthAlertThreshold> saved = thresholds.stream().map(t -> {
            String dataType = (String) t.get("dataType");
            HealthAlertThreshold entity = repo.findByDataType(dataType).orElse(new HealthAlertThreshold());
            entity.setDataType(dataType);
            if (t.get("minValue") != null) {
                entity.setMinValue(Double.valueOf(t.get("minValue").toString()));
            }
            if (t.get("maxValue") != null) {
                entity.setMaxValue(Double.valueOf(t.get("maxValue").toString()));
            }
            if (t.get("enabled") != null) {
                entity.setEnabled(Boolean.parseBoolean(t.get("enabled").toString()));
            }
            entity.setUpdatedAt(new Date());
            return repo.save(entity);
        }).collect(Collectors.toList());

        List<HealthAlertThresholdDto> dtos = saved.stream()
                .map(HealthAlertThresholdDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}