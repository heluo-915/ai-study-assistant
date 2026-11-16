package com.example.aistudyassistant.service;

import com.example.aistudyassistant.entity.User;
import com.example.aistudyassistant.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    // 通过构造方法接收并保存所需依赖或初始化字段。
    public AuthService(
            PasswordEncoder passwordEncoder,
            UserRepository userRepository
    ) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }
    // 对密码进行 BCrypt 编码并调用用户仓库保存。
    public void register(String username, String password) {
        String passwordHash = passwordEncoder.encode(password);
        userRepository.save(username, passwordHash);
    }
    // 根据用户名查询用户并校验密码。
    public Optional<User> authenticate(String username, String password) {
        Optional<User> user = userRepository.findByUsername(username);

        if (user.isEmpty()
                || !passwordEncoder.matches(password, user.get().getPasswordHash())) {
            return Optional.empty();
        }

        return user;
    }
}