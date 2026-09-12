package com.ucm.controller;

import com.ucm.common.ApiResponse;
import com.ucm.entity.SysUser;
import com.ucm.repository.SysUserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** 用户查询（派单下拉等） */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final SysUserRepository userRepo;

    public UserController(SysUserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @GetMapping
    public ApiResponse<?> list() {
        return ApiResponse.ok(userRepo.findAll().stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("realName", u.getRealName());
            m.put("role", u.getRole().name());
            return m;
        }).toList());
    }
}
