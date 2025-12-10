package com.example.demo.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 可选的 SPA 回退控制器。当属性 app.spa.enabled=true 时，
 * 将若干前端页面路由（/、/login、/register、/home）转发到静态的 /index.html，
 * 便于渐进式切换到 Vue SPA，而无需删除或修改现有的模板文件。
 *
 * 使用方法：在 src/main/resources/application.properties 中设置：
 *   app.spa.enabled=true
 *
 * 注意：默认不启用（false），以保持现有行为不变。启用后请确保前端构建产物已部署到 static 目录。
 */
@Controller
@ConditionalOnProperty(prefix = "app.spa", name = "enabled", havingValue = "true")
public class SpaFallbackController {
    
    @GetMapping("/")
    public String root() {
        return "redirect:/index.html";
    }
    
    @GetMapping({"/login", "/register", "/home", "/menu"})
    public String spaRoutes() {
        return "redirect:/index.html";
    }
}
