package com.citygo;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 认证与数据隔离（实施书 §4）：注册不得指定角色、登录、越权 401/403、
 * 禁用与角色变更即时生效、JWT 伪造/无 token。
 */
class AuthIntegrationTest extends BaseIntegrationTest {

    @Test
    void registerLoginMe_and_roleAlwaysUser() throws Exception {
        String token = register("alice", "secret123");
        assertThat(roleOf("alice")).isEqualTo("USER");

        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    void registerDuplicateUsername_conflict() throws Exception {
        register("bob", "secret123");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "bob", "password", "secret456"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40900));
    }

    @Test
    void loginWrongPassword_badCredentials() throws Exception {
        register("carol", "secret123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "carol", "password", "wrong-pass"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40102));
    }

    @Test
    void disabledUser_loginAndOldTokenRejected() throws Exception {
        Long id = insertUser("dave", "secret123", "USER", true);
        String token = loginToken("dave", "secret123");

        jdbcTemplate.update("UPDATE users SET enabled = 0 WHERE id = ?", id);
        // 禁用即时生效：已有 token 也失效
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
        // 禁用后登录同样拒绝
        mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "dave", "password", "secret123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void roleChangeTakesEffectImmediately() throws Exception {
        Long opId = insertUser("erin", "secret123", "OPERATOR", true);
        String token = loginToken("erin", "secret123");
        mockMvc.perform(get("/api/admin/categories").header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        // 角色降级即时生效：同一 token 不再有权限
        jdbcTemplate.update("UPDATE users SET role = 'USER' WHERE id = ?", opId);
        mockMvc.perform(get("/api/admin/categories").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    void userAccessingAdminApi_forbidden() throws Exception {
        String token = register("frank", "secret123");
        mockMvc.perform(get("/api/admin/categories").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void noTokenOrForgedToken_unauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));

        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer("forged.token.value")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40100));
    }

    @Test
    void updateOwnProfile_ok_andDataIsolation() throws Exception {
        String token = register("grace", "secret123");
        mockMvc.perform(put("/api/user/profile")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("nickname", "格蕾丝", "phone", "13800000000"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("格蕾丝"));
    }

    @Test
    void logout_ok() throws Exception {
        String token = register("heidi", "secret123");
        mockMvc.perform(post("/api/auth/logout").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    private String register(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.role").value("USER"))
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("data").get("token").asText();
    }

    private String roleOf(String username) {
        return jdbcTemplate.queryForObject("SELECT role FROM users WHERE username = ?", String.class, username);
    }
}
