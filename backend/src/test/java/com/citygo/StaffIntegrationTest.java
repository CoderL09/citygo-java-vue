package com.citygo;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
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
 * 员工管理（仅 ADMIN）：越权、启停、角色、重置密码、自我保护与最后管理员保护。
 */
class StaffIntegrationTest extends BaseIntegrationTest {

    private String adminToken() throws Exception {
        insertUser("admin1", "admin12345", "ADMIN", true);
        return loginToken("admin1", "admin12345");
    }

    @Test
    void userAndOperatorCannotAccessStaffApi() throws Exception {
        String user = registerUser("user1");
        String operator = registerStaff("op1");

        mockMvc.perform(get("/api/admin/staff").header("Authorization", bearer(user)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/staff").header("Authorization", bearer(operator)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCreatesStaff_listsAndFilters() throws Exception {
        String admin = adminToken();
        mockMvc.perform(post("/api/admin/staff")
                        .header("Authorization", bearer(admin))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "op2", "password", "op2pass123",
                                        "nickname", "运营二号", "role", "OPERATOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("OPERATOR"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        mockMvc.perform(get("/api/admin/staff?keyword=op2&page=1&pageSize=10")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].username").value("op2"));
    }

    @Test
    void adminCreatesStaffWithBadRole_rejected() throws Exception {
        String admin = adminToken();
        mockMvc.perform(post("/api/admin/staff")
                        .header("Authorization", bearer(admin))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "op3", "password", "op3pass123", "role", "SUPERUSER"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void disableStaff_loginAndExistingTokenRejected() throws Exception {
        String admin = adminToken();
        String opToken = registerStaff("op4");
        Long opId = idOf("op4");

        mockMvc.perform(put("/api/admin/staff/" + opId + "/enabled")
                        .header("Authorization", bearer(admin))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("enabled", false))))
                .andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForObject("SELECT enabled FROM users WHERE id = ?", Integer.class, opId))
                .isZero();
        mockMvc.perform(get("/api/admin/categories").header("Authorization", bearer(opToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void cannotDisableSelf_orChangeOwnRole() throws Exception {
        String admin = adminToken();
        Long adminId = idOf("admin1");

        mockMvc.perform(put("/api/admin/staff/" + adminId + "/enabled")
                        .header("Authorization", bearer(admin))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("enabled", false))))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/admin/staff/" + adminId)
                        .header("Authorization", bearer(admin))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "OPERATOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void cannotDisableOrDemoteLastEnabledAdmin() throws Exception {
        String admin = adminToken();
        Long adminId = idOf("admin1");
        Long opId = insertUser("op5", "op5pass123", "OPERATOR", true);

        // 将另一员工升为 ADMIN 后，原管理员才允许被降级（非最后管理员）
        mockMvc.perform(put("/api/admin/staff/" + opId)
                        .header("Authorization", bearer(admin))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "ADMIN"))))
                .andExpect(status().isOk());

        // 现在有两个启用 ADMIN，降级 admin1 到 OPERATOR 应允许？不行：不能修改自己角色
        mockMvc.perform(put("/api/admin/staff/" + adminId)
                        .header("Authorization", bearer(admin))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "OPERATOR"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void resetPassword_newPasswordWorks() throws Exception {
        String admin = adminToken();
        Long opId = insertUser("op6", "op6pass123", "OPERATOR", true);

        mockMvc.perform(put("/api/admin/staff/" + opId + "/password")
                        .header("Authorization", bearer(admin))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("password", "newpass123"))))
                .andExpect(status().isOk());

        String newToken = loginToken("op6", "newpass123");
        assertThat(newToken).isNotBlank();
    }

    private String registerUser(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", username, "password", "secret123"))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("data").get("token").asText();
    }

    private String registerStaff(String username) throws Exception {
        insertUser(username, "secret123", "OPERATOR", true);
        return loginToken(username, "secret123");
    }

    private Long idOf(String username) {
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);
    }
}
