package com.citygo.report;
import com.citygo.common.api.ApiResponse;
import com.alibaba.excel.EasyExcel;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;
@RestController @RequestMapping("/api/admin/reports") @RequiredArgsConstructor public class ReportController {
 private final ReportService service;
 @GetMapping public ApiResponse<?> report(@RequestParam LocalDate from,@RequestParam LocalDate to){return ApiResponse.ok(service.report(from,to));}
 @GetMapping("/export")public void export(@RequestParam LocalDate from,@RequestParam LocalDate to,HttpServletResponse response)throws java.io.IOException{
 var r=service.report(from,to);response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");response.setHeader("Content-Disposition","attachment; filename=citygo-report.xlsx");
 try(var writer=EasyExcel.write(response.getOutputStream()).autoCloseStream(false).build()){
 List<List<String>> head=List.of(List.of("上海日期"),List.of("支付金额(分)"),List.of("退款金额(分)"),List.of("净收款(分)"),List.of("新增用户"),List.of("新订单"),List.of("取消关闭"),List.of("子场次预约名额"),List.of("已确认订单"),List.of("已付款订单"));
 writer.write(r.days().stream().map(d->List.of(d.date().toString(),d.paidCents(),d.refundedCents(),d.netCents(),d.newUsers(),d.orders(),d.cancelledClosed(),d.reservedPeople(),d.confirmedOrders(),d.eligibleOrders())).toList(),EasyExcel.writerSheet(0,"每日统计").head(head).build());
 writer.write(r.rankings().stream().map(a->List.of(a.itemType(),a.itemId(),ReportService.safeCell(a.name()),a.quantity(),a.salesCents())).toList(),EasyExcel.writerSheet(1,"商品毛销售排名").head(List.of(List.of("类型"),List.of("商品ID"),List.of("名称"),List.of("数量"),List.of("销售额(分)"))).build());
 writer.write(List.of(List.of(r.definition())),EasyExcel.writerSheet(2,"口径").head(List.of(List.of("统计说明"))).build());}
 }
}
