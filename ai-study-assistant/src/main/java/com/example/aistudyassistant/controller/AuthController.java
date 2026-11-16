package com.example.aistudyassistant.controller;

import com.example.aistudyassistant.dto.LoginRequest;
import com.example.aistudyassistant.dto.RegisterRequest;
import com.example.aistudyassistant.entity.User;
import com.example.aistudyassistant.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.dao.DuplicateKeyException;
import com.example.aistudyassistant.service.AuthService;
import com.example.aistudyassistant.dto.LoginResponse;
import com.example.aistudyassistant.service.JwtService;

import java.util.Optional;

@RestController
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;

    // 通过构造方法接收并保存所需依赖或初始化字段。
    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }
    @PostMapping("/register")
    // 校验注册数据并保存密码哈希，用户名冲突时返回 409。
    public ResponseEntity<String> register(@RequestBody RegisterRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body("用户名和密码不能为空");
        }
        if (username.length() > 50) {
            return ResponseEntity.badRequest().body("用户名不能超过 50 个字符");
        }

        try {
            authService.register(username, password);
        } catch (DuplicateKeyException e) {
            return ResponseEntity.status(409).body("用户名已存在");
        }

        return ResponseEntity.status(201).body("注册成功");

    }
    @PostMapping("/login")
    // 验证用户名和密码，成功后签发 JWT。
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body("用户名和密码不能为空");
        }

        Optional<User> user = authService.authenticate(username, password);

        if (user.isEmpty()) {
            return ResponseEntity.status(401).body("用户名或密码错误");
        }

        String token = jwtService.generateToken(user.get().getId());
        return ResponseEntity.ok(new LoginResponse(token));
    }
}