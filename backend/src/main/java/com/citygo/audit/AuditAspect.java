package com.citygo.audit;
import com.citygo.auth.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.*;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
@Aspect @Component @Order(10) @RequiredArgsConstructor public class AuditAspect {
 private final AuditWriter writer;
 @Around("execution(public * com.citygo..controller.*.*(..)) || execution(public * com.citygo.report.ReportController.*(..))")
 public Object record(ProceedingJoinPoint p)throws Throwable{
 var auth=SecurityContextHolder.getContext().getAuthentication();if(auth==null||!(auth.getPrincipal() instanceof LoginUser user)||"USER".equals(user.role()))return p.proceed();
 var method=((MethodSignature)p.getSignature()).getMethod();boolean modify=method.isAnnotationPresent(PostMapping.class)||method.isAnnotationPresent(PutMapping.class)||method.isAnnotationPresent(DeleteMapping.class)||method.getName().equals("export");if(!modify)return p.proceed();
 Long id=null;var params=method.getParameters();var args=p.getArgs();for(int i=0;i<params.length;i++)if(params[i].isAnnotationPresent(PathVariable.class)&&args[i] instanceof Long v){id=v;break;}
 String action=method.getName(),type=method.getDeclaringClass().getSimpleName();Object value;
 try{value=p.proceed();}catch(Throwable failure){try{writer.write(user.id(),action,type,id,false,"操作失败："+failure.getClass().getSimpleName());}catch(RuntimeException logFailure){failure.addSuppressed(logFailure);}throw failure;}
 // Only allowlisted operation metadata is recorded: never serialize request bodies or exception messages.
 if("redeem".equals(action)&&value instanceof com.citygo.common.api.ApiResponse<?> response&&response.getData() instanceof com.citygo.booking.service.BookingService.Detail detail)id=detail.order().id();
 writer.write(user.id(),action,type,id,true,"操作成功");return value;
 }
}
