package com.ucm.config;

import com.ucm.controller.AuthController;
import com.ucm.entity.SysUser;
import com.ucm.repository.SysUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** 初始化种子数据：默认账号 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final SysUserRepository userRepo;

    public DataInitializer(SysUserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public void run(String... args) {
        if (userRepo.count() > 0) return;
        SysUser admin = new SysUser();
        admin.setUsername("admin");
        admin.setPassword(AuthController.sha256("admin123"));
        admin.setRealName("系统管理员");
        admin.setRole(SysUser.Role.ADMIN);
        userRepo.save(admin);

        SysUser zhangsan = new SysUser();
        zhangsan.setUsername("zhangsan");
        zhangsan.setPassword(AuthController.sha256("123456"));
        zhangsan.setRealName("张三");
        zhangsan.setRole(SysUser.Role.OPERATOR);
        userRepo.save(zhangsan);

        log.info("初始化账号完成: admin/admin123, zhangsan/123456");
    }
}
