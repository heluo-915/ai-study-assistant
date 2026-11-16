package com.example.aistudyassistant.repository;

import com.example.aistudyassistant.entity.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    // 通过构造方法接收并保存所需依赖或初始化字段。
    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    // 将用户名和密码哈希写入数据库。
    public void save(String username, String passwordHash) {
        jdbcTemplate.update(
                "INSERT INTO users (username, password_hash) VALUES (?, ?)",
                username,
                passwordHash
        );
    }
    // 通过用户名查找用户并返回可选结果。
    public Optional<User> findByUsername(String username) {
        List<User> users = jdbcTemplate.query(
                "SELECT id, username, password_hash FROM users WHERE username = ?",
                (resultSet, rowNum) -> {
                    User user = new User();
                    user.setId(resultSet.getLong("id"));
                    user.setUsername(resultSet.getString("username"));
                    user.setPasswordHash(resultSet.getString("password_hash"));
                    return user;
                },
                username
        );

        return users.stream().findFirst();
    }
}