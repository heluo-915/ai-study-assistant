package com.example.aistudyassistant.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DatabaseCheckController {

    private final JdbcTemplate jdbcTemplate;

    // 通过构造方法接收并保存所需依赖或初始化字段。
    public DatabaseCheckController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/database-check")
    // 执行 SELECT 1 检查数据库连接。
    public String check() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        return "数据库查询结果：" + result;
    }
    @GetMapping("/users/count")
    // 查询用户总数。
    public Integer countUsers() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users",
                Integer.class
        );
    }
}