package com.example.demo.controller;

import com.example.demo.service.MySqlService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/db")
public class DbTestController {

    private final MySqlService mySqlService;

    public DbTestController(MySqlService mySqlService) {
        this.mySqlService = mySqlService;
    }

    @GetMapping("/test")
    public Map<String, Object> test() {
        Map<String, Object> result = new HashMap<>();
        try {
            boolean ok = mySqlService.testConnection();
            result.put("ok", ok);
            result.put("message", ok ? "connection ok" : "connection failed");
        } catch (Exception e) {
            result.put("ok", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
