package com.ucm.controller;

import com.ucm.common.ApiResponse;
import com.ucm.common.BizException;
import com.ucm.common.UserContext;
import com.ucm.entity.SysUser;
import com.ucm.repository.SysUserRepository;
import com.ucm.service.TokenService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final SysUserRepository userRepo;
    private final TokenService tokenService;

    public AuthController(SysUserRepository userRepo, TokenService tokenService) {
        this.userRepo = userRepo;
        this.tokenService = tokenService;
    }

    public record LoginReq(@NotBlank String username, @NotBlank String password) {}

    @PostMapping("/login")
    public ApiResponse<?> login(@RequestBody LoginReq req) {
        SysUser user = userRepo.findByUsername(req.username())
                .orElseThrow(() -> new BizException("用户名或密码错误"));
        if (!user.getPassword().equals(sha256(req.password()))) {
            throw new BizException("用户名或密码错误");
        }
        String token = tokenService.issue(user);
        return ApiResponse.ok(Map.of(
                "token", token,
                "user", Map.of("id", user.getId(), "username", user.getUsername(),
                        "realName", user.getRealName() == null ? "" : user.getRealName(),
                        "role", user.getRole().name())
        ));
    }

    @GetMapping("/profile")
    public ApiResponse<?> profile() {
        SysUser u = UserContext.get();
        return ApiResponse.ok(Map.of("id", u.getId(), "username", u.getUsername(),
                "realName", u.getRealName() == null ? "" : u.getRealName(),
                "role", u.getRole().name()));
    }

    public static String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
