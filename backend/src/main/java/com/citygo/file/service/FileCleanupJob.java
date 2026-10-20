package com.citygo.file.service;
import com.citygo.file.mapper.FileObjectMapper;
import com.citygo.file.storage.StorageService;
import com.citygo.payment.mapper.LifecycleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
@Component @RequiredArgsConstructor public class FileCleanupJob {
 private final JdbcTemplate db;private final FileObjectMapper files;private final StorageService storage;private final PlatformTransactionManager manager;private final LifecycleMapper jobs;private final Clock clock;
 @Value("${citygo.jobs.enabled:true}")boolean enabled;
 @Scheduled(fixedDelayString="${citygo.jobs.file-cleanup-delay-ms:3600000}",initialDelay=60000)public void scheduled(){if(enabled)cleanup();}
 public void cleanup(){var now=LocalDateTime.now(clock);db.update("UPDATE file_objects f SET status='TEMP',unreferenced_at=? WHERE status='USED' AND NOT EXISTS(SELECT 1 FROM activity_images i WHERE i.file_object_id=f.id)",now);
 for(Long id:db.queryForList("SELECT id FROM file_objects WHERE status='DELETING' OR (status='TEMP' AND unreferenced_at<=?) ORDER BY id LIMIT 100",Long.class,now.minusHours(24)))try{
 TransactionTemplate tx=new TransactionTemplate(manager);String key=tx.execute(s->{var f=files.lockById(id);if(f==null||files.countImageRefs(id)>0)return null;Integer due=db.queryForObject("SELECT COUNT(*) FROM file_objects WHERE id=? AND (status='DELETING' OR (status='TEMP' AND unreferenced_at<=?))",Integer.class,id,now.minusHours(24));if(due==0)return null;files.requestDelete(id,now);return f.getStorageKey();});
 if(key==null)continue;storage.delete(key);tx.executeWithoutResult(s->{var f=files.lockById(id);if(f!=null&&"DELETING".equals(f.getStatus())&&files.countImageRefs(id)==0){files.deleteById(id);db.update("INSERT INTO audit_logs(operator_id,action,target_type,target_id,summary,success,created_at) VALUES(0,'cleanup','FileObject',?,'保留窗口后清理孤立文件',1,?)",id,now);}});
 }catch(RuntimeException e){jobs.jobFailure("file-cleanup",id,e.getClass().getSimpleName(),now);}}
}
