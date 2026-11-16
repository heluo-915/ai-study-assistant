package com.example.aistudyassistant.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;

    // 通过构造方法接收并保存所需依赖或初始化字段。
    public JwtService(@Value("${app.jwt.secret}") String encodedSecret) {
        byte[] keyBytes = Decoders.BASE64.decode(encodedSecret);//把它从 Base64 字符串还原成字节
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    // 签发包含用户 ID 和过期时间的 JWT。
    public String generateToken(Long userId) {
        Instant now = Instant.now();  // 获取当前时间

        return Jwts.builder()
                .subject(userId.toString())                    // 写入用户 ID
                .issuedAt(Date.from(now))                     // 设置签发时间
                .expiration(Date.from(now.plusSeconds(3600)))  // 设置过期时间
                .signWith(secretKey, Jwts.SIG.HS256)           // 使用密钥签名
                .compact();                                   // 生成最终 token 字符串
    }
    // 校验 JWT 签名与有效期并解析用户 ID。
    public Long parseUserId(String token) {
        var claims = Jwts.parser()          // 1. 开始配置 JWT 解析器
                .verifyWith(secretKey)      // 2. 指定验证签名使用的密钥
                .build()                    // 3. 创建解析器
                .parseSignedClaims(token)   // 4. 解析令牌，并进行签名、过期时间等检查
                .getPayload();              // 5. 取得令牌中的载荷内容

        if (claims.getExpiration() == null) {
            throw new IllegalArgumentException("令牌缺少过期时间");
        }

        return Long.valueOf(claims.getSubject());
    }
}