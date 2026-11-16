package com.example.aistudyassistant.security;

import com.example.aistudyassistant.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.cors.CorsUtils;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtService jwtService;

    // 通过构造方法接收并保存所需依赖或初始化字段。
    public JwtInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    // 读取并校验 Bearer 令牌，成功时将用户 ID 写入请求属性。
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        // 预检请求只决定浏览器能否发起请求，不授予实际接口访问权限。
        if (CorsUtils.isPreFlightRequest(request)) return true;
        String authorization = request.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        String token = authorization.substring(7);

        try {
            Long userId = jwtService.parseUserId(token);
            request.setAttribute("userId", userId);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
    }
}
