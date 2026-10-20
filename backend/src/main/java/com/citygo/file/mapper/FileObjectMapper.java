package com.citygo.file.mapper;

import com.citygo.file.entity.FileObject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FileObjectMapper {

    @org.apache.ibatis.annotations.Select("SELECT * FROM file_objects WHERE id=#{id} FOR UPDATE") FileObject lockById(Long id);
    @org.apache.ibatis.annotations.Update("UPDATE file_objects SET status='DELETING',delete_requested_at=#{now} WHERE id=#{id}") int requestDelete(@Param("id")Long id,@Param("now")java.time.LocalDateTime now);
    int insert(FileObject fileObject);

    FileObject findById(@Param("id") Long id);

    int updateStatus(@Param("id") Long id, @Param("status") String status);

    int deleteById(@Param("id") Long id);

    long countImageRefs(@Param("fileObjectId") Long fileObjectId);
}
