package com.citygo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 集成测试基类：真实 MySQL（test profile → citygo_test），每用例前清库。
 * 清库顺序按外键依赖从子到父；platform_settings 种子行保留并复位。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    private static final List<String> TABLES = List.of(
            "order_checkins", "checkin_tickets", "order_reschedules", "order_fulfillments", "audit_logs", "job_failures", "payment_anomalies", "mock_gateway_refunds", "mock_gateway_trades", "refunds", "payment_attempts", "order_events", "order_allocations", "order_items",
            "orders", "cart_items", "contacts", "notification_recipients", "notifications",
            "outbox_events", "activity_images", "file_objects", "package_items", "sessions",
            "packages", "activities", "categories", "users");

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired protected org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    @BeforeEach
    void cleanDatabase() {
        // Fail before the first DELETE if a local environment override points at another database.
        String database = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        if (!"citygo_test".equals(database)) {
            throw new IllegalStateException("Integration tests require citygo_test; refusing database cleanup");
        }
        try(var cursor=redisTemplate.scan(org.springframework.data.redis.core.ScanOptions.scanOptions().match("citygo:test:*").count(100).build())){
            java.util.List<String> owned=new java.util.ArrayList<>();cursor.forEachRemaining(owned::add);if(!owned.isEmpty())redisTemplate.delete(owned);
        }
        TABLES.forEach(t -> jdbcTemplate.execute("DELETE FROM " + t));
        jdbcTemplate.update("UPDATE platform_settings SET setting_value = 'true', updated_by = NULL "
                + "WHERE setting_key = 'accepting_orders'");
    }

    protected Long insertUser(String username, String password, String role, boolean enabled) {
        jdbcTemplate.update("INSERT INTO users (username, password_hash, nickname, role, enabled, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                username, passwordEncoder.encode(password), username, role, enabled,
                LocalDateTime.now(), LocalDateTime.now());
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = ?", Long.class, username);
    }

    protected String loginToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("data").get("token").asText();
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }
}
