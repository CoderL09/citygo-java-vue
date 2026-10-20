package com.citygo;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class ContactCartPackageIntegrationTest extends P2Fixtures {
    @Test void contactDefaultsAndOwnership() throws Exception {
        insertUser("contactuser","secret123","USER",true);String token=loginToken("contactuser","secret123");
        String first=mockMvc.perform(post("/api/contacts").header("Authorization",bearer(token)).contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(Map.of("name","首位","phone","13800000000","defaultContact",false))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.defaultContact").value(true)).andReturn().getResponse().getContentAsString();
        long id=objectMapper.readTree(first).at("/data/id").asLong();
        mockMvc.perform(post("/api/contacts").header("Authorization",bearer(token)).contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(Map.of("name","第二位","phone","13900000000","defaultContact",true)))).andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM contacts WHERE is_default=1",Integer.class)).isEqualTo(1);
        insertUser("alien","secret123","USER",true);String alien=loginToken("alien","secret123");
        mockMvc.perform(delete("/api/contacts/"+id).header("Authorization",bearer(alien))).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/contacts").header("Authorization",bearer(alien))).andExpect(jsonPath("$.data.length()").value(0));
        long current=jdbcTemplate.queryForObject("SELECT id FROM contacts WHERE is_default=1",Long.class);
        mockMvc.perform(delete("/api/contacts/"+current).header("Authorization",bearer(token))).andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForObject("SELECT id FROM contacts WHERE is_default=1",Long.class)).isEqualTo(id);
    }
    @Test void cartMergesEnforcesLimitsReportsInvalidAndKeepsOwnership() throws Exception {
        insertUser("cartuser","secret123","USER",true);String token=loginToken("cartuser","secret123");long s=session(10,1234);
        for(int i=0;i<2;i++)mockMvc.perform(post("/api/cart").header("Authorization",bearer(token)).contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(Map.of("itemType","SESSION","itemId",s,"quantity",2)))).andExpect(status().isOk());
        assertThat(count("cart_items")).isEqualTo(1);long id=jdbcTemplate.queryForObject("SELECT id FROM cart_items",Long.class);
        mockMvc.perform(get("/api/cart").header("Authorization",bearer(token))).andExpect(jsonPath("$.data[0].item.quantity").value(4)).andExpect(jsonPath("$.data[0].product.unitPriceCents").value(1234));
        mockMvc.perform(post("/api/cart").header("Authorization",bearer(token)).contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(Map.of("itemType","SESSION","itemId",s,"quantity",2)))).andExpect(status().isBadRequest());
        insertUser("cartalien","secret123","USER",true);String other=loginToken("cartalien","secret123");
        mockMvc.perform(put("/api/cart/"+id).header("Authorization",bearer(other)).contentType(APPLICATION_JSON).content("{\"quantity\":1,\"selected\":false}")).andExpect(status().isNotFound());
        jdbcTemplate.update("UPDATE sessions SET status='OFF_SALE' WHERE id=?",s);
        mockMvc.perform(get("/api/cart").header("Authorization",bearer(token))).andExpect(jsonPath("$.data[0].valid").value(false));
        mockMvc.perform(put("/api/cart/"+id).header("Authorization",bearer(token)).contentType(APPLICATION_JSON).content("{\"quantity\":4,\"selected\":false}")).andExpect(status().isOk());
        mockMvc.perform(delete("/api/cart").header("Authorization",bearer(token))).andExpect(status().isOk());assertThat(count("cart_items")).isZero();
    }
    @Test void packageVersionsDoNotOverwriteOldPriceOrComposition_andCategoryDeletionIsRestricted() throws Exception {
        insertUser("packadmin","secret123","ADMIN",true);String admin=loginToken("packadmin","secret123");long c=category("PACKAGE"),a=session(10,1000),b=session(10,2000);
        String created=mockMvc.perform(post("/api/admin/packages").header("Authorization",bearer(admin)).contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(Map.of("categoryId",c,"name","城市组合","packagePriceCents",1500,"sessionIds",List.of(a,b)))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.product.status").value("OFF_SALE")).andReturn().getResponse().getContentAsString();
        long id=objectMapper.readTree(created).at("/data/product/id").asLong();
        mockMvc.perform(get("/api/packages/"+id)).andExpect(status().isNotFound());
        mockMvc.perform(put("/api/admin/packages/"+id+"/status").header("Authorization",bearer(admin)).contentType(APPLICATION_JSON).content("{\"status\":\"ON_SALE\"}")).andExpect(status().isOk());
        mockMvc.perform(get("/api/packages/"+id)).andExpect(jsonPath("$.data.sessions.length()").value(2));
        mockMvc.perform(post("/api/admin/packages/"+id+"/versions").header("Authorization",bearer(admin)).contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(Map.of("categoryId",c,"name","城市组合新版","packagePriceCents",1800,"sessionIds",List.of(b)))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.product.version").value(2)).andExpect(jsonPath("$.data.sessions.length()").value(1));
        mockMvc.perform(get("/api/packages/"+id)).andExpect(jsonPath("$.data.product.packagePriceCents").value(1500)).andExpect(jsonPath("$.data.sessions.length()").value(2));
        mockMvc.perform(delete("/api/admin/categories/"+c).header("Authorization",bearer(admin))).andExpect(status().isConflict());
        mockMvc.perform(delete("/api/admin/sessions/"+a).header("Authorization",bearer(admin))).andExpect(status().isConflict());
        mockMvc.perform(post("/api/admin/packages").header("Authorization",bearer(admin)).contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(Map.of("categoryId",c,"name","重复场次","packagePriceCents",1500,"sessionIds",List.of(a,a))))).andExpect(status().isBadRequest());
    }
}
