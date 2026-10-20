package com.citygo;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 分类：CRUD、排序、启停、关联删除限制（40901）、公开列表仅启用项。
 */
class CategoryIntegrationTest extends BaseIntegrationTest {

    @org.junit.jupiter.api.Test
    void invalidPublicPaginationReturnsBadRequest() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/categories?page=0"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/categories?pageSize=101"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
    }


    private String adminToken() throws Exception {
        insertUser("cadmin", "admin12345", "ADMIN", true);
        return loginToken("cadmin", "admin12345");
    }

    private long createCategory(String admin, String type, String name) throws Exception {
        String body = mockMvc.perform(post("/api/admin/categories")
                        .header("Authorization", bearer(admin))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("categoryType", type, "name", name, "sortOrder", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNumber())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("data").get("id").asLong();
    }

    @Test
    void createUpdateSortDisablePublicVisibility() throws Exception {
        String admin = adminToken();
        long id = createCategory(admin, "ACTIVITY", "展览");

        mockMvc.perform(get("/api/categories?type=ACTIVITY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));

        mockMvc.perform(put("/api/admin/categories/" + id)
                        .header("Authorization", bearer(admin))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "艺术展览", "sortOrder", 9))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("艺术展览"));

        // 停用后公开列表不可见，管理端可见
        mockMvc.perform(put("/api/admin/categories/" + id + "/status")
                        .header("Authorization", bearer(admin))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", false))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/categories?type=ACTIVITY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));

        mockMvc.perform(get("/api/admin/categories?type=ACTIVITY&status=false")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void deleteWithoutChildren_ok() throws Exception {
        String admin = adminToken();
        long id = createCategory(admin, "PACKAGE", "套票");
        mockMvc.perform(delete("/api/admin/categories/" + id).header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void deleteWithActivities_conflict40901() throws Exception {
        String admin = adminToken();
        long id = createCategory(admin, "ACTIVITY", "演出");
        jdbcTemplate.update("INSERT INTO activities (category_id, name, description, address, status, created_at, updated_at) "
                        + "VALUES (?, '测试活动', NULL, '某地', 'ON_SALE', NOW(3), NOW(3))", id);

        mockMvc.perform(delete("/api/admin/categories/" + id).header("Authorization", bearer(admin)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40901));
    }

    @Test
    void invalidType_rejected() throws Exception {
        String admin = adminToken();
        mockMvc.perform(post("/api/admin/categories")
                        .header("Authorization", bearer(admin))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("categoryType", "FOOD", "name", "食物"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unauthenticatedCannotManage() throws Exception {
        mockMvc.perform(post("/api/admin/categories")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("categoryType", "ACTIVITY", "name", "展览"))))
                .andExpect(status().isUnauthorized());
    }
}
