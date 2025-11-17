package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

/**
 * 主菜单控制器
 *
 * 说明：
 * - 提供 /menu 页面，用于在用户登录成功后显示主菜单。
 * - 在视图中展示当前登录用户名（如果存在）。
 * - 该控制器使用 Spring MVC 的注解式声明，返回模板名 `menu`（对应 resources/templates/menu.html）。
 */
@Controller
public class MenuController {

    /**
     * 处理 /menu GET 请求，渲染主菜单页面。
     * @param model    用于向视图传递数据
     * @param principal 表示当前认证用户（如果未认证则为 null）
     * @return 返回模板名 "menu"，由 Thymeleaf 或其他模板引擎解析
     */
    @GetMapping("/menu")
    public String menu(Model model, Principal principal) {
        String username = (principal != null) ? principal.getName() : "匿名";
        model.addAttribute("username", username);
        return "menu";
    }
}

