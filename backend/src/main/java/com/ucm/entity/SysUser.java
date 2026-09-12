package com.ucm.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 系统用户（城管管理员 / 执法队员） */
@Entity
@Table(name = "sys_user")
public class SysUser {

    public enum Role { ADMIN, OPERATOR }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(nullable = false, length = 64)
    private String password; // SHA-256 摘要

    @Column(name = "real_name", length = 64)
    private String realName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Role role = Role.OPERATOR;

    @Column(length = 32)
    private String phone;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
