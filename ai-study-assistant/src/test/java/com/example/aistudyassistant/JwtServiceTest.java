package com.example.aistudyassistant;

import com.example.aistudyassistant.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    @Test
    // 验证过期 JWT 会被拒绝。
    void expiredTokenShouldBeRejected() {
        // 固定的测试密钥，仅用于测试，不用于真实运行
        byte[] keyBytes = new byte[32];
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);

        String encodedKey = Base64.getEncoder().encodeToString(keyBytes);
        JwtService jwtService = new JwtService(encodedKey);

        Instant now = Instant.now();

        String expiredToken = Jwts.builder()
                .subject("1")
                .issuedAt(Date.from(now.minusSeconds(120)))
                .expiration(Date.from(now.minusSeconds(60)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertThrows(
                ExpiredJwtException.class,
                () -> jwtService.parseUserId(expiredToken)
        );
    }

    @Test
    // 验证有效 JWT 可以解析出用户 ID。
    void validTokenShouldReturnUserId() {
        byte[] keyBytes = new byte[32];
        String encodedKey = Base64.getEncoder().encodeToString(keyBytes);
        JwtService jwtService = new JwtService(encodedKey);

        String token = jwtService.generateToken(1L);
        Long userId = jwtService.parseUserId(token);

        assertEquals(1L, userId);
    }
}