package com.citygo.catalog.mapper;
import com.citygo.catalog.entity.TravelPackage;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.time.LocalDateTime;
@Mapper
public interface PackageMapper {
    @Select("SELECT * FROM packages WHERE id = #{id}") TravelPackage find(Long id);
    @Select("SELECT * FROM packages WHERE id = #{id} FOR UPDATE") TravelPackage lock(Long id);
    @Select("SELECT * FROM packages WHERE id = #{id} FOR SHARE") TravelPackage share(Long id);
    @Select("SELECT session_id FROM package_items WHERE package_id = #{id} ORDER BY session_id") List<Long> sessions(Long id);
    @Select("<script>SELECT * FROM packages WHERE 1=1 <if test='status != null'>AND status=#{status}</if> <if test='categoryId != null'>AND category_id=#{categoryId}</if> <if test='keyword != null'>AND name LIKE CONCAT('%',#{keyword},'%')</if> ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}</script>")
    List<TravelPackage> page(@Param("status") String status,@Param("categoryId") Long categoryId,@Param("keyword") String keyword,@Param("offset") int offset,@Param("limit") int limit);
    @Select("<script>SELECT COUNT(*) FROM packages WHERE 1=1 <if test='status != null'>AND status=#{status}</if> <if test='categoryId != null'>AND category_id=#{categoryId}</if> <if test='keyword != null'>AND name LIKE CONCAT('%',#{keyword},'%')</if></script>")
    long count(@Param("status") String status,@Param("categoryId") Long categoryId,@Param("keyword") String keyword);
    @Insert("INSERT INTO packages(category_id,name,description,package_price_cents,version,family_id,status,created_by,created_at,updated_at) VALUES(#{categoryId},#{name},#{description},#{packagePriceCents},#{version},#{familyId},#{status},#{createdBy},#{createdAt},#{updatedAt})")
    @Options(useGeneratedKeys=true,keyProperty="id") int insert(TravelPackage item);
    @Update("UPDATE packages SET family_id=#{id} WHERE id=#{id} AND family_id IS NULL") int initFamily(Long id);
    @Select("SELECT COALESCE(MAX(version),0) FROM packages WHERE family_id=#{familyId}") int maxVersion(Long familyId);
    @Insert("INSERT INTO package_items(package_id,session_id,sort_order,created_at) VALUES(#{packageId},#{sessionId},#{sortOrder},#{now})")
    int addSession(@Param("packageId") Long packageId,@Param("sessionId") Long sessionId,@Param("sortOrder") int sortOrder,@Param("now") LocalDateTime now);
    @Update("UPDATE packages SET status=#{status},updated_at=#{now} WHERE id=#{id}")
    int status(@Param("id") Long id,@Param("status") String status,@Param("now") LocalDateTime now);
    @Select("SELECT COUNT(*) FROM packages WHERE category_id=#{id}") long countCategory(Long id);
    @Select("SELECT COUNT(*) FROM package_items WHERE session_id=#{id}") long countSession(Long id);
}
