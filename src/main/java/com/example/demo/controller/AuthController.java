package com.example.demo.controller;

import com.example.demo.service.AuthService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/home";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/home")
    public String homePage() {
        return "home";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(@RequestParam String username,
                             @RequestParam String password,
                             @RequestParam(name = "confirmPassword", required = false) String confirmPassword,
                             @RequestParam(name = "email", required = false) String email,
                             Model model,
                             RedirectAttributes ra) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            model.addAttribute("error", "用户名和密码不能为空");
            return "register";
        }
        if (confirmPassword != null && !password.equals(confirmPassword)) {
            model.addAttribute("error", "两次密码不一致");
            return "register";
        }
        try {
            authService.registerNewUser(username.trim(), password, email);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "register";
        } catch (Exception ex) {
            model.addAttribute("error", "注册失败，请稍后重试");
            return "register";
        }
        ra.addFlashAttribute("msg", "注册成功，请登录");
        return "redirect:/login";
    }
}
