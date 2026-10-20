package com.citygo.auth.service;

import com.citygo.auth.entity.User;
import com.citygo.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 本地初始化管理员（实施书 §4：管理员通过本地配置建立，不提交生产通用密码）。
 * 环境变量 CITYGO_ADMIN_USERNAME / CITYGO_ADMIN_PASSWORD 未配置时不创建，
 * 仅提示；生产环境由部署方另行建立。测试 profile 下不执行（测试自建账号）。
 */
@Slf4j
@Component
@Profile("dev & !prod")
@RequiredArgsConstructor
public class AdminInitRunner implements ApplicationRunner {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    @Value("${CITYGO_ADMIN_USERNAME:}")
    private String adminUsername;

    @Value("${CITYGO_ADMIN_PASSWORD:}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (adminUsername == null || adminUsername.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            log.warn("CITYGO_ADMIN_USERNAME / CITYGO_ADMIN_PASSWORD 未配置，跳过管理员初始化；"
                    + "生产环境请通过部署流程单独建立管理员账号");
            return;
        }
        if (userMapper.countByRole("ADMIN") > 0) {
            log.info("已存在 ADMIN 账号，跳过初始化");
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setNickname("系统管理员");
        admin.setRole("ADMIN");
        admin.setEnabled(true);
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);
        userMapper.insert(admin);
        log.warn("已按本地配置创建管理员账号 {}（密码来自 .env，勿提交）", adminUsername);
    }
}
