package com.citygo.audit;
import com.citygo.common.api.*;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
@RestController @RequestMapping("/api/admin/audit") @Validated @RequiredArgsConstructor public class AuditController {
 private final JdbcTemplate db;public record Row(Long id,Long operatorId,String action,String targetType,Long targetId,String summary,boolean success,LocalDateTime createdAt){}
 @GetMapping public ApiResponse<?> page(@RequestParam(defaultValue="1")@Min(1)int page,@RequestParam(defaultValue="20")@Min(1)@Max(100)int pageSize){long offset=(long)(page-1)*pageSize;var rows=db.query("SELECT * FROM audit_logs ORDER BY id DESC LIMIT ? OFFSET ?",(r,i)->new Row(r.getLong("id"),r.getLong("operator_id"),r.getString("action"),r.getString("target_type"),r.getObject("target_id",Long.class),r.getString("summary"),r.getBoolean("success"),r.getTimestamp("created_at").toLocalDateTime()),pageSize,offset);return ApiResponse.ok(PageResult.of(rows,db.queryForObject("SELECT COUNT(*) FROM audit_logs",Long.class),page,pageSize));}
}
