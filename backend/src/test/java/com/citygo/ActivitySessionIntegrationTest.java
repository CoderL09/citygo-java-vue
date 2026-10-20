package com.citygo;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 活动与场次：上下架、公开可见性、场次时间/名额校验、
 * 有效订单引用禁止修改/删除（40902）、活动删除级联。
 */
class ActivitySessionIntegrationTest extends BaseIntegrationTest {

    private String admin;

    private String adminToken() throws Exception {
        insertUser("sadmin", "admin12345", "ADMIN", true);
        return loginToken("sadmin", "admin12345");
    }

    private long createCategory(String type) {
        jdbcTemplate.update("INSERT INTO categories (category_type, name, sort_order, status, created_at, updated_at) "
                + "VALUES (?, ?, 0, 1, NOW(3), NOW(3))", type, "分类" + type);
        return jdbcTemplate.queryForObject("SELECT id FROM categories ORDER BY id DESC LIMIT 1", Long.class);
    }

    private long createActivity() throws Exception {
        long categoryId = createCategory("ACTIVITY");
        MvcResult result = mockMvc.perform(post("/api/admin/activities")
                        .header("Authorization", bearer(admin))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "categoryId", categoryId, "name", "博物馆奇妙夜",
                                "description", "夜游活动", "address", "市博物馆"))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asLong();
    }

    private Map<String, Object> sessionBody() {
        String fmt = "yyyy-MM-dd'T'HH:mm:ss'Z'";
        return Map.of(
                "startTime", LocalDateTime.now().plusDays(5).format(DateTimeFormatter.ofPattern(fmt)),
                "endTime", LocalDateTime.now().plusDays(5).plusHours(2).format(DateTimeFormatter.ofPattern(fmt)),
                "bookingDeadline", LocalDateTime.now().plusDays(4).format(DateTimeFormatter.ofPattern(fmt)),
                "priceCents", 9900, "capacity", 50);
    }

    @Test
    void createSession_publicDetailShowsSessions() throws Exception {
        admin = adminToken();
        long activityId = createActivity();

        mockMvc.perform(post("/api/admin/activities/" + activityId + "/sessions")
                        .header("Authorization", bearer(admin))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sessionBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(50))
                .andExpect(jsonPath("$.data.priceCents").value(9900));

        mockMvc.perform(get("/api/activities/" + activityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessions.length()").value(1))
                .andExpect(jsonPath("$.data.sessions[0].status").value("ON_SALE"));
    }

    @Test
    void sessionTimeValidation_rejected() throws Exception {
        admin = adminToken();
        long activityId = createActivity();

        // 结束早于开始
        mockMvc.perform(post("/api/admin/activities/" + activityId + "/sessions")
                        .header("Authorization", bearer(admin))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "startTime", "2026-11-01T10:00:00Z", "endTime", "2026-11-01T09:00:00Z",
                                "bookingDeadline", "2026-10-30T10:00:00Z", "priceCents", 100, "capacity", 10))))
                .andExpect(status().isBadRequest());

        // 截止晚于开始
        mockMvc.perform(post("/api/admin/activities/" + activityId + "/sessions")
                        .header("Authorization", bearer(admin))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "startTime", "2026-11-01T10:00:00Z", "endTime", "2026-11-01T12:00:00Z",
                                "bookingDeadline", "2026-11-02T10:00:00Z", "priceCents", 100, "capacity", 10))))
                .andExpect(status().isBadRequest());

        // 负价格 / 零容量
        mockMvc.perform(post("/api/admin/activities/" + activityId + "/sessions")
                        .header("Authorization", bearer(admin))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "startTime", "2026-11-01T10:00:00Z", "endTime", "2026-11-01T12:00:00Z",
                                "bookingDeadline", "2026-10-30T10:00:00Z", "priceCents", -1, "capacity", 10))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/admin/activities/" + activityId + "/sessions")
                        .header("Authorization", bearer(admin))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "startTime", "2026-11-01T10:00:00Z", "endTime", "2026-11-01T12:00:00Z",
                                "bookingDeadline", "2026-10-30T10:00:00Z", "priceCents", 100, "capacity", 0))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void offSaleActivity_hiddenFromPublic() throws Exception {
        admin = adminToken();
        long activityId = createActivity();

        mockMvc.perform(put("/api/admin/activities/" + activityId + "/status")
                        .header("Authorization", bearer(admin))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "OFF_SALE"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/activities/" + activityId))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/activities?page=1&pageSize=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    void sessionWithValidOrders_cannotUpdateOrDelete() throws Exception {
        admin = adminToken();
        long activityId = createActivity();

        String body = mockMvc.perform(post("/api/admin/activities/" + activityId + "/sessions")
                        .header("Authorization", bearer(admin))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sessionBody())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long sessionId = objectMapper.readTree(body).get("data").get("id").asLong();

        // 造一个有效订单引用（P2 起由真实下单产生）
        insertOrderWithItem(sessionId);

        mockMvc.perform(put("/api/admin/sessions/" + sessionId)
                        .header("Authorization", bearer(admin))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sessionBody())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40902));

        mockMvc.perform(delete("/api/admin/sessions/" + sessionId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40902));

        // 活动删除同样受限
        mockMvc.perform(delete("/api/admin/activities/" + activityId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40902));
    }

    @Test
    void adminCanReadOffSaleDetail_butUserCannot() throws Exception {
        admin = adminToken();
        long id = createActivity();
        jdbcTemplate.update("UPDATE activities SET status = 'OFF_SALE' WHERE id = ?", id);
        mockMvc.perform(get("/api/admin/activities/" + id).header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("OFF_SALE"));
        mockMvc.perform(get("/api/activities/" + id)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/activities/" + id + "/sessions")).andExpect(status().isNotFound());
        insertUser("viewer", "secret123", "USER", true);
        mockMvc.perform(get("/api/admin/activities/" + id)
                .header("Authorization", bearer(loginToken("viewer", "secret123"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void packageCategoryCannotBeUsedForActivity() throws Exception {
        admin = adminToken();
        long category = createCategory("PACKAGE");
        mockMvc.perform(post("/api/admin/activities").header("Authorization", bearer(admin))
                .contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(Map.of(
                        "categoryId", category, "name", "错误类型", "address", "地址"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sessionOffsetIsNormalizedToUtc_andMissingOffsetRejected() throws Exception {
        admin = adminToken();
        long id = createActivity();
        mockMvc.perform(post("/api/admin/activities/" + id + "/sessions")
                .header("Authorization", bearer(admin)).contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "startTime", "2027-01-01T10:00:00+08:00", "endTime", "2027-01-01T12:00:00+08:00",
                    "bookingDeadline", "2026-12-31T10:00:00+08:00", "priceCents", 100, "capacity", 10))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.startTime").value("2027-01-01T02:00Z"));
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT start_time FROM sessions WHERE activity_id = ?", java.time.LocalDateTime.class, id))
                .isEqualTo(java.time.LocalDateTime.of(2027, 1, 1, 2, 0));
        mockMvc.perform(post("/api/admin/activities/" + id + "/sessions")
                .header("Authorization", bearer(admin)).contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "startTime", "2027-01-01T10:00:00", "endTime", "2027-01-01T12:00:00Z",
                    "bookingDeadline", "2026-12-31T10:00:00Z", "priceCents", 100, "capacity", 10))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void activityDeleteWithoutOrders_cascades() throws Exception {
        admin = adminToken();
        long activityId = createActivity();

        mockMvc.perform(delete("/api/admin/activities/" + activityId)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk());

        Integer sessions = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sessions WHERE activity_id = ?", Integer.class, activityId);
        assertThat(sessions).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM activities WHERE id = ?", Integer.class, activityId)).isZero();
    }

    private void insertOrderWithItem(long sessionId) {
        jdbcTemplate.update("INSERT INTO users (username, password_hash, nickname, role, enabled, created_at, updated_at) "
                        + "VALUES ('buyer1', 'x', '买家', 'USER', 1, NOW(3), NOW(3))");
        long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'buyer1'", Long.class);
        jdbcTemplate.update("INSERT INTO orders (order_no, user_id, total_cents, status, contact_name, contact_phone, created_at, updated_at) "
                        + "VALUES ('T202610200001', ?, 9900, 'PENDING_CONFIRMATION', '张三', '13800000000', NOW(3), NOW(3))", userId);
        long orderId = jdbcTemplate.queryForObject("SELECT id FROM orders WHERE order_no = 'T202610200001'", Long.class);
        jdbcTemplate.update("INSERT INTO order_items (order_id, item_type, item_id, name, session_id, quantity, unit_price_cents, line_total_cents, created_at) "
                        + "VALUES (?, 'SESSION', ?, '测试场次', ?, 1, 9900, 9900, NOW(3))", orderId, sessionId, sessionId);
    }
}
