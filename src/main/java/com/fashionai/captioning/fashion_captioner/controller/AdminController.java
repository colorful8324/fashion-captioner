package com.fashionai.captioning.fashion_captioner.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {
    @GetMapping("dashboard")
    public String dashboard() {
        return "admin/dashboard";
    }
    @GetMapping("category")
    public String category() {
        return "admin/category";
    }
    @GetMapping("product")
    public String product() {
        return "admin/product";
    }
    @GetMapping("login")
    public String tables() {
        return "login";
    }
    @GetMapping("user")
    public String user() {
        return "admin/user";
    }
}