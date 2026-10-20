package com.citygo.audit;
import com.citygo.auth.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.*;
import org.aspectj.lang.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.Map;
@Aspect @Component @Order(210) @RequiredArgsConstructor public class OperatorFieldsAspect {
 private final JdbcTemplate db;
 private final Map<String,String> tables=Map.of("CategoryService","categories","ActivityService","activities","SessionService","sessions","PackageService","packages","StaffService","users");
 @Around("@annotation(org.springframework.transaction.annotation.Transactional) && (execution(* com.citygo.catalog.service.*.*(..)) || execution(* com.citygo.staff.service.*.*(..)))")
 public Object fill(ProceedingJoinPoint p)throws Throwable{Object result=p.proceed();var a=SecurityContextHolder.getContext().getAuthentication();if(a==null||!(a.getPrincipal() instanceof LoginUser u))return result;String name=p.getSignature().getName(),type=p.getSignature().getDeclaringType().getSimpleName(),table=tables.get(type);if(table==null||!java.util.Set.of("create","update","setStatus","status","setEnabled","resetPassword").contains(name))return result;
 if(!TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Operator metadata requires business transaction");Long id=null;if(result instanceof com.citygo.catalog.service.PackageService.PackageDetail detail)id=detail.product().getId();if(id==null&&result!=null)for(String getter:java.util.List.of("id","getId"))try{Object value=result.getClass().getMethod(getter).invoke(result);if(value instanceof Long v){id=v;break;}}catch(ReflectiveOperationException ignored){}
 if(id==null&&!name.equals("create")){int position=type.equals("StaffService")&&!name.equals("resetPassword")?1:0;Object[] args=p.getArgs();if(args.length>position&&args[position] instanceof Long v)id=v;}
 if(id!=null){if(name.equals("create"))db.update("UPDATE "+table+" SET created_by=?,updated_by=? WHERE id=?",u.id(),u.id(),id);else db.update("UPDATE "+table+" SET updated_by=? WHERE id=?",u.id(),id);}return result;}
}
