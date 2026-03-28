package com.project.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    @GetMapping("/admin")
    public String dashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/admin/login")
    public String login() {
        return "admin/login";
    }
}
