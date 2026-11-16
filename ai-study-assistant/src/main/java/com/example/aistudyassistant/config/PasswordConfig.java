package com.example.aistudyassistant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    @Bean
    // 创建 BCrypt 密码编码器供注册和登录使用。
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}