package com.citygo.report;
import com.citygo.common.api.ErrorCode;
import com.citygo.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
@Service @RequiredArgsConstructor public class ReportService {
 private final JdbcTemplate db;
 public record Window(LocalDate from,LocalDate to,LocalDateTime start,LocalDateTime end){}
 public record Daily(LocalDate date,long paidCents,long refundedCents,long netCents,long newUsers,long orders,long cancelledClosed,long reservedPeople,long confirmedOrders,long eligibleOrders){}
 public record Ranking(String itemType,Long itemId,String name,long quantity,long salesCents){}
 public record Report(List<Daily> days,List<Ranking> rankings,String definition){}
 public Window window(LocalDate from,LocalDate to){if(from==null||to==null||to.isBefore(from)||ChronoUnit.DAYS.between(from,to)>92)throw new BizException(ErrorCode.BAD_REQUEST,"请选择最多 93 个自然日");ZoneId z=ZoneId.of("Asia/Shanghai");return new Window(from,to,LocalDateTime.ofInstant(from.atStartOfDay(z).toInstant(),ZoneOffset.UTC),LocalDateTime.ofInstant(to.plusDays(1).atStartOfDay(z).toInstant(),ZoneOffset.UTC));}
 private long n(String sql,Object... args){Long v=db.queryForObject(sql,Long.class,args);return v==null?0:v;}
 // A repeatable-read snapshot keeps JSON and export internally consistent while payments continue.
 @Transactional(readOnly=true,isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
 public Report report(LocalDate from,LocalDate to){Window w=window(from,to);List<Daily> days=new ArrayList<>();for(LocalDate d=from;!d.isAfter(to);d=d.plusDays(1)){Window a=window(d,d);Object[] t={a.start(),a.end()};long pay=n("SELECT COALESCE(SUM(amount_cents),0) FROM payment_attempts WHERE status='SUCCESS' AND paid_at>=? AND paid_at<?",t),refund=n("SELECT COALESCE(SUM(amount_cents),0) FROM refunds WHERE status='SUCCESS' AND success_at>=? AND success_at<?",t);
 long orders=n("SELECT COUNT(*) FROM orders WHERE created_at>=? AND created_at<?",t),eligible=n("SELECT COUNT(*) FROM orders WHERE paid_at IS NOT NULL AND created_at>=? AND created_at<?",t),confirmed=n("SELECT COUNT(*) FROM orders WHERE confirmed_at IS NOT NULL AND paid_at IS NOT NULL AND created_at>=? AND created_at<?",t);
 days.add(new Daily(d,pay,refund,pay-refund,n("SELECT COUNT(*) FROM users WHERE role='USER' AND created_at>=? AND created_at<?",t),orders,n("SELECT COUNT(*) FROM orders WHERE (cancelled_at>=? AND cancelled_at<?) OR (closed_at>=? AND closed_at<?)",a.start(),a.end(),a.start(),a.end()),n("SELECT COALESCE(SUM(a.quantity),0) FROM order_allocations a JOIN orders o ON o.id=a.order_id WHERE o.created_at>=? AND o.created_at<?",t),confirmed,eligible));}
 List<Ranking> ranks=db.query("SELECT i.item_type,i.item_id,MAX(i.name) name,SUM(i.quantity) qty,SUM(i.line_total_cents) sales FROM order_items i JOIN orders o ON o.id=i.order_id WHERE o.paid_at>=? AND o.paid_at<? GROUP BY i.item_type,i.item_id ORDER BY sales DESC,i.item_type,i.item_id LIMIT 1000",(r,i)->new Ranking(r.getString("item_type"),r.getLong("item_id"),r.getString("name"),r.getLong("qty"),r.getLong("sales")),w.start(),w.end());
 return new Report(days,ranks,"上海自然日；支付按成功支付 paid_at，退款按成功 success_at（含补偿）；净收款=支付-退款。订单/新增用户/子场次预约名额按创建日；取消关闭按实际发生日；确认率为创建日已确认订单/已付款订单（历史确认保留）。排名按订单 paid_at 的商品毛销量/原价销售额，含后续退款；套餐单列，子场次名额不另算销售金额。最多 93 日、排名 1000 行。");}
 public static String safeCell(String s){if(s==null)return "";String trimmed=s.stripLeading();return !trimmed.isEmpty()&&"=+-@".indexOf(trimmed.charAt(0))>=0?"'"+s:s;}
}
