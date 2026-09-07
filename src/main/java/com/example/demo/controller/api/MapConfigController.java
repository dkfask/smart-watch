package com.example.demo.controller.api;

import com.example.demo.service.TiandituLocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 地图前端公共配置接口
 */
@RestController
@RequestMapping("/api/config")
public class MapConfigController {

    private final TiandituLocationService tiandituLocationService;

    public MapConfigController(TiandituLocationService tiandituLocationService) {
        this.tiandituLocationService = tiandituLocationService;
    }

    @GetMapping("/map")
    public ResponseEntity<?> getMapConfig() {
        return ResponseEntity.ok(Map.of(
                "provider", "tianditu",
                "tiandituKey", tiandituLocationService.getKey()
        ));
    }
}
