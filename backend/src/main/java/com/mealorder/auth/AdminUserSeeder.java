package com.mealorder.auth;

import com.mealorder.auth.entity.AdminUser;
import com.mealorder.auth.mapper.AdminUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * admin_user 种子：空表时注入两个内置账号（密码 BCrypt 加密后落库）
 * admin/admin123(ADMIN) merchant/merchant123(MERCHANT)，仅 dev 环境
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminUserSeeder implements CommandLineRunner {

    private final AdminUserMapper adminUserMapper;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        if (adminUserMapper.selectCount(null) > 0) {
            return;
        }
        insert("admin", "admin123", "系统管理员", "ADMIN");
        insert("merchant", "merchant123", "示例商家", "MERCHANT");
        log.info("admin_user 种子写入完成: admin/admin123, merchant/merchant123");
    }

    private void insert(String username, String rawPassword, String nickname, String role) {
        AdminUser user = new AdminUser();
        user.setUsername(username);
        user.setPassword(encoder.encode(rawPassword));
        user.setNickname(nickname);
        user.setRole(role);
        user.setStatus(1);
        adminUserMapper.insert(user);
    }
}
