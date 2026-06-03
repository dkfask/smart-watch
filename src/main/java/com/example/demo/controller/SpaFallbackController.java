package com.example.demo.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SPA回退控制器，将所有前端路由转发到index.html
 * 启用条件：app.spa.enabled=true
 */
@Controller
@ConditionalOnProperty(prefix = "app.spa", name = "enabled", havingValue = "true")
public class SpaFallbackController {

    @GetMapping("/")
    public String root() {
        return "redirect:/index.html";
    }

    @GetMapping({
            "/login", "/register", "/home",
            "/devices", "/patients", "/fences", "/alarm", "/alarms",
            "/realtime", "/history", "/dashboard"
    })
    public String spaRoutes() {
        return "redirect:/index.html";
    }

    @GetMapping("/history/**")
    public String historyRoutes() {
        return "redirect:/index.html";
    }
}
