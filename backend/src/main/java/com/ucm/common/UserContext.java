package com.ucm.common;

import com.ucm.entity.SysUser;

/** 当前登录用户上下文（由 AuthInterceptor 注入） */
public final class UserContext {

    private static final ThreadLocal<SysUser> HOLDER = new ThreadLocal<>();

    private UserContext() {}

    public static void set(SysUser user) { HOLDER.set(user); }

    public static SysUser get() { return HOLDER.get(); }

    public static String username() {
        SysUser u = HOLDER.get();
        return u == null ? "system" : u.getUsername();
    }

    public static void clear() { HOLDER.remove(); }
}
