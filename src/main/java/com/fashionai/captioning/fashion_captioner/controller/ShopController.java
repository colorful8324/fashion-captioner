package com.fashionai.captioning.fashion_captioner.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class ShopController {

    @GetMapping({"/", "index"})
    public String home() {
        return "shop/index";
    }

    @GetMapping("/shop")
    public String shop() {
        return "shop/shop";
    }

    @GetMapping("/about")
    public String about() {
        return "shop/about";
    }

    @GetMapping("/recommendation")
    public String blog() {
        return "shop/recommendation";
    }

    @PostMapping("/recommendation/caption")
    public String caption() {
        return "shop/caption";
    }

    @GetMapping("/cart")
    public String cart() {
        return "shop/cart";
    }

    @GetMapping("/checkout")
    public String checkout() {
        return "shop/checkout";
    }

    @GetMapping("/contact")
    public String contact() {
        return "shop/contact";
    }

    @GetMapping("/services")
    public String services() {
        return "shop/services";
    }


}