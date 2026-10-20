package com.citygo.audit;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
@Service @RequiredArgsConstructor public class AuditWriter {private final JdbcTemplate db;private final Clock clock;
 @Transactional(propagation=Propagation.REQUIRES_NEW) public void write(Long uid,String action,String type,Long target,boolean success,String summary){db.update("INSERT INTO audit_logs(operator_id,action,target_type,target_id,summary,success,created_at) VALUES(?,?,?,?,?,?,?)",uid,action,type,target,summary,success,LocalDateTime.now(clock));}
}
