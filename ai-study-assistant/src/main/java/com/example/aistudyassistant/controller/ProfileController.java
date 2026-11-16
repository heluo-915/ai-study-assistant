package com.example.aistudyassistant.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProfileController {

    @GetMapping("/profile")
    // 返回拦截器解析出的当前用户 ID。
    public String profile(@RequestAttribute("userId") Long userId) {
        return "当前用户 ID：" + userId;
    }
}