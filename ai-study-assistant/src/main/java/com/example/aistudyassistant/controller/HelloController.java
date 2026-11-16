package com.example.aistudyassistant.controller;

import com.example.aistudyassistant.dto.HelloRequest;
import com.example.aistudyassistant.dto.HelloResponse;
import org.springframework.web.bind.annotation.*;

@RestController
public class HelloController {

    @GetMapping("/hello")
    // 处理 GET 问候请求并返回响应对象。
    public HelloResponse hello(@RequestParam(defaultValue = "同学") String name) {
        return new HelloResponse(name, "Hello, " + name + "!");
    }
    @PostMapping("/hello")
    // 处理 POST 问候请求并为缺失名字提供默认值。
    public HelloResponse greet(@RequestBody HelloRequest request) {
        String name = request.getName();
        if (name == null || name.isBlank()) {
            name = "同学";
        }
        return new HelloResponse(name, "Hello, " + name + "!");
    }
}