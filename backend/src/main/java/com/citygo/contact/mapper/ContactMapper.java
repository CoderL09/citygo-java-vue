package com.citygo.contact.mapper;
import com.citygo.contact.entity.Contact;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper
public interface ContactMapper {
    @Select("SELECT id,user_id,name,phone,is_default AS default_contact,created_at,updated_at FROM contacts WHERE user_id=#{uid} ORDER BY is_default DESC,id") List<Contact> list(Long uid);
    @Select("SELECT id,user_id,name,phone,is_default AS default_contact,created_at,updated_at FROM contacts WHERE id=#{id} AND user_id=#{uid}") Contact find(@Param("uid") Long uid,@Param("id") Long id);
    @Update("UPDATE contacts SET is_default=0 WHERE user_id=#{uid} AND is_default=1") int clearDefault(Long uid);
    @Insert("INSERT INTO contacts(user_id,name,phone,is_default,created_at,updated_at) VALUES(#{userId},#{name},#{phone},#{defaultContact},#{createdAt},#{updatedAt})")
    @Options(useGeneratedKeys=true,keyProperty="id") int insert(Contact c);
    @Update("UPDATE contacts SET name=#{name},phone=#{phone},is_default=#{defaultContact},updated_at=#{updatedAt} WHERE id=#{id} AND user_id=#{userId}") int update(Contact c);
    @Delete("DELETE FROM contacts WHERE id=#{id} AND user_id=#{uid}") int delete(@Param("uid") Long uid,@Param("id") Long id);
}
