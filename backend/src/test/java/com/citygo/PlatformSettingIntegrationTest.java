package com.citygo;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 平台接单开关（营业状态）：公开查询、仅 ADMIN 修改、OPERATOR/USER 拒绝。
 */
class PlatformSettingIntegrationTest extends BaseIntegrationTest {

    @Test
    void publicQuery_defaultTrue() throws Exception {
        mockMvc.perform(get("/api/settings/accepting-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.acceptingOrders").value(true));
    }

    @Test
    void adminTogglesSwitch_publicReflects() throws Exception {
        insertUser("opadmin", "secret123", "ADMIN", true);
        String token = loginToken("opadmin", "secret123");

        mockMvc.perform(put("/api/admin/settings/accepting-orders")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("acceptingOrders", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.acceptingOrders").value(false));

        mockMvc.perform(get("/api/settings/accepting-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.acceptingOrders").value(false));
    }

    @Test
    void operatorCannotToggle() throws Exception {
        insertUser("operator", "secret123", "OPERATOR", true);
        String token = loginToken("operator", "secret123");
        mockMvc.perform(put("/api/admin/settings/accepting-orders")
                .header("Authorization", bearer(token)).contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("acceptingOrders", false))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/settings/accepting-orders"))
                .andExpect(jsonPath("$.data.acceptingOrders").value(true));
    }

    @Test
    void userCannotToggle() throws Exception {
        insertUser("plainuser", "secret123", "USER", true);
        String token = loginToken("plainuser", "secret123");

        mockMvc.perform(put("/api/admin/settings/accepting-orders")
                        .header("Authorization", bearer(token))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("acceptingOrders", false))))
                .andExpect(status().isForbidden());
    }
}
