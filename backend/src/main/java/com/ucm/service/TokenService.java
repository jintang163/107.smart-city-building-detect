package com.ucm.service;

import com.ucm.entity.SysUser;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 内存 Token 服务（演示版；生产可换 JWT/Redis） */
@Service
public class TokenService {

    private final Map<String, SysUser> tokens = new ConcurrentHashMap<>();

    public String issue(SysUser user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, user);
        return token;
    }

    public SysUser resolve(String token) {
        if (token == null || token.isBlank()) return null;
        return tokens.get(token);
    }

    public void revoke(String token) {
        if (token != null) tokens.remove(token);
    }
}
