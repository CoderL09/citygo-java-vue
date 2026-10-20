package com.citygo.payment.gateway;
import com.citygo.common.api.ErrorCode;
import com.citygo.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.*;
import java.util.*;

/** Durable provider simulation. All calls deliberately reject execution under business transaction locks. */
@Component @Profile({"dev","test"}) @RequiredArgsConstructor
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="citygo.payment.mock-enabled",havingValue="true")
public class MockPaymentGateway implements PaymentGateway {
    private final JdbcTemplate db; private final Clock clock;
    private void outside(){if(TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("Gateway called inside business transaction");}
    public Result pay(Request r,String scenario){
        outside();String status=switch(scenario){case "SUCCESS","DELAYED"->"SUCCESS";case "FAIL"->"FAILED";case "TIMEOUT"->"PENDING";default->throw new BizException(ErrorCode.BAD_REQUEST,"模拟场景无效");};
        insert("mock_gateway_trades",r,status);
        Result result=read("mock_gateway_trades",r);
        return "DELAYED".equals(scenario)?new Result(r.requestNo(),r.orderNo(),r.amountCents(),"UNKNOWN",null,null):result;
    }
    public Result query(Request r){outside();return read("mock_gateway_trades",r);}
    public Result close(Request r){outside();insert("mock_gateway_trades",r,"CLOSED");db.update("UPDATE mock_gateway_trades SET status='CLOSED' WHERE request_no=? AND status='PENDING'",r.requestNo());return read("mock_gateway_trades",r);}
    public Result refund(Request r,String original){
        outside();List<Map<String,Object>> paid=db.queryForList("SELECT * FROM mock_gateway_trades WHERE request_no=? AND status='SUCCESS'",original);
        if(paid.isEmpty() || ((Number)paid.getFirst().get("amount_cents")).intValue()!=r.amountCents() || !r.orderNo().equals(paid.getFirst().get("order_no")))throw new BizException(ErrorCode.REFUND_AMOUNT_MISMATCH);
        insert("mock_gateway_refunds",r,"SUCCESS");return read("mock_gateway_refunds",r);
    }
    public Result queryRefund(Request r){outside();return read("mock_gateway_refunds",r);}
    /** Only tests and authenticated, signed provider notifications use this; no arbitrary HTTP control endpoint. */
    public void acceptPaid(Result r){outside();db.update("INSERT INTO mock_gateway_trades(request_no,order_no,amount_cents,status,external_no,occurred_at) VALUES(?,?,?,'SUCCESS',?,?) ON DUPLICATE KEY UPDATE status='SUCCESS',external_no=VALUES(external_no),occurred_at=VALUES(occurred_at)",r.requestNo(),r.orderNo(),r.amountCents(),r.externalNo(),r.occurredAt());}
    private void insert(String table,Request r,String status){db.update("INSERT INTO "+table+"(request_no,order_no,amount_cents,status,external_no,occurred_at) VALUES(?,?,?,?,?,?) ON DUPLICATE KEY UPDATE request_no=request_no",r.requestNo(),r.orderNo(),r.amountCents(),status,"MOCK_"+r.requestNo(),"SUCCESS".equals(status)?LocalDateTime.now(clock):null);}
    private Result read(String table,Request r){
        List<Result> rows=db.query("SELECT * FROM "+table+" WHERE request_no=?",(rs,n)->new Result(rs.getString("request_no"),rs.getString("order_no"),rs.getInt("amount_cents"),rs.getString("status"),rs.getString("external_no"),rs.getObject("occurred_at",LocalDateTime.class)),r.requestNo());
        return rows.isEmpty()?new Result(r.requestNo(),r.orderNo(),r.amountCents(),"UNKNOWN",null,null):rows.getFirst();
    }
}
