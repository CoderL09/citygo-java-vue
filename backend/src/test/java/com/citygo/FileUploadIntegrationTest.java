package com.citygo;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 文件上传安全（实施书 §12）：类型白名单、魔数校验、大小限制、
 * 伪造扩展名拒绝、路径穿越拒绝、权限与引用保护。
 */
class FileUploadIntegrationTest extends BaseIntegrationTest {

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    private byte[] pngBytes() {
        try{var out=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",out);return out.toByteArray();}catch(java.io.IOException e){throw new IllegalStateException(e);}
    }

    private String adminToken() throws Exception {
        insertUser("fadmin", "admin12345", "ADMIN", true);
        return loginToken("fadmin", "admin12345");
    }

    private String userToken() throws Exception {
        insertUser("fuser", "secret123", "USER", true);
        return loginToken("fuser", "secret123");
    }

    private String upload(String token, String filename, byte[] content) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", filename, "image/png", content);
        MvcResult result = mockMvc.perform(multipart("/api/admin/files").file(file)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("data").get("url").asText();
    }

    @Test
    void uploadValidPng_thenPublicRead() throws Exception {
        String admin = adminToken();
        String url = upload(admin, "cover.png", pngBytes());
        assertThat(url).startsWith("/api/files/");

        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentType()).contains("image/png"));
    }

    @Test
    void fakeExtension_rejected() throws Exception {
        String admin = adminToken();
        // 文本内容冒充 png
        MockMultipartFile file = new MockMultipartFile("file", "fake.png", "image/png",
                "this is not a real png file, just plain text padding".getBytes());
        mockMvc.perform(multipart("/api/admin/files").file(file)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void disallowedExtension_rejected() throws Exception {
        String admin = adminToken();
        MockMultipartFile file = new MockMultipartFile("file", "script.exe",
                "application/octet-stream", "MZ...".getBytes());
        mockMvc.perform(multipart("/api/admin/files").file(file)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void oversizeFile_rejected() throws Exception {
        String admin = adminToken();
        byte[] big = new byte[6 * 1024 * 1024];
        System.arraycopy(PNG_SIGNATURE, 0, big, 0, 8);
        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", big);
        mockMvc.perform(multipart("/api/admin/files").file(file)
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void pathTraversalLoad_rejected() throws Exception {
        mockMvc.perform(get("/api/files/..%2F..%2F..%2Fwindows%2Fsystem32"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void userCannotUpload() throws Exception {
        String user = userToken();
        MockMultipartFile file = new MockMultipartFile("file", "cover.png", "image/png", pngBytes());
        mockMvc.perform(multipart("/api/admin/files").file(file)
                        .header("Authorization", bearer(user)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteReferencedFile_conflict() throws Exception {
        String admin = adminToken();
        // 先建分类+活动，挂接图片后文件进入 USED，删除被拒
        long categoryId = insertCategory();
        long activityId = insertActivity(categoryId);
        long fileId = uploadAndGetId(admin);

        mockMvc.perform(post("/api/admin/activities/" + activityId + "/images")
                        .header("Authorization", bearer(admin))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("fileObjectId", fileId, "sortOrder", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value(org.hamcrest.Matchers.startsWith("/api/files/")));

        mockMvc.perform(delete("/api/admin/files/" + fileId).header("Authorization", bearer(admin)))
                .andExpect(status().isConflict());
    }

    @Test
    void operatorCannotDeleteOrAttachAnotherEmployeesFile() throws Exception {
        String admin = adminToken();
        long fileId = uploadAndGetId(admin);
        long activityId = insertActivity(insertCategory());
        insertUser("fileop", "secret123", "OPERATOR", true);
        String operator = loginToken("fileop", "secret123");
        mockMvc.perform(delete("/api/admin/files/" + fileId).header("Authorization", bearer(operator)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/activities/" + activityId + "/images")
                .header("Authorization", bearer(operator)).contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("fileObjectId", fileId))))
                .andExpect(status().isForbidden());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM file_objects WHERE id = ?",
                Integer.class, fileId)).isEqualTo(1);
    }

    private long insertCategory() {
        jdbcTemplate.update("INSERT INTO categories (category_type, name, sort_order, status, created_at, updated_at) "
                + "VALUES ('ACTIVITY', '分类', 0, 1, NOW(3), NOW(3))");
        return jdbcTemplate.queryForObject("SELECT id FROM categories ORDER BY id DESC LIMIT 1", Long.class);
    }

    private long insertActivity(long categoryId) {
        jdbcTemplate.update("INSERT INTO activities (category_id, name, description, address, status, created_at, updated_at) "
                + "VALUES (?, '活动', NULL, '地址', 'ON_SALE', NOW(3), NOW(3))", categoryId);
        return jdbcTemplate.queryForObject("SELECT id FROM activities ORDER BY id DESC LIMIT 1", Long.class);
    }

    private long uploadAndGetId(String token) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "cover.png", "image/png", pngBytes());
        MvcResult result = mockMvc.perform(multipart("/api/admin/files").file(file)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asLong();
    }
}
