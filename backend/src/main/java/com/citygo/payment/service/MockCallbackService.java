package com.citygo.payment.service;
import com.citygo.payment.gateway.PaymentGateway.Result;
import com.citygo.payment.gateway.MockPaymentGateway;
import com.citygo.common.api.ErrorCode;
import com.citygo.common.exception.BizException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
@Service @Profile({"dev","test"})
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="citygo.payment.mock-enabled",havingValue="true")
public class MockCallbackService {
    private final PaymentService service;private final MockPaymentGateway gateway;private final ObjectMapper json;private final String secret;
    public MockCallbackService(PaymentService service,MockPaymentGateway gateway,ObjectMapper json,@Value("${CITYGO_MOCK_CALLBACK_SECRET:${citygo.jwt.secret}}")String secret){this.service=service;this.gateway=gateway;this.json=json;this.secret=secret;}
    public String signature(Result r){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return HexFormat.of().formatHex(mac.doFinal(json.writeValueAsBytes(r)));}catch(Exception e){throw new IllegalStateException(e);}}
    public void receiveRefund(Result r,String signature){verify(r,signature);service.settleRefundByNo(r);}
    private void verify(Result r,String signature){
        if(r==null||signature==null||!MessageDigest.isEqual(signature(r).getBytes(StandardCharsets.UTF_8),signature.getBytes(StandardCharsets.UTF_8)))throw new BizException(ErrorCode.FORBIDDEN,"回调签名无效");
    }
    public void receive(Result r,String signature){
        verify(r,signature);
        if(signature==null||!MessageDigest.isEqual(signature(r).getBytes(StandardCharsets.UTF_8),signature.getBytes(StandardCharsets.UTF_8)))throw new BizException(ErrorCode.FORBIDDEN,"回调签名无效");
        service.validatePayment(r);if("SUCCESS".equals(r.status()))gateway.acceptPaid(r);
        Long refund=service.settlePayment(r);if(refund!=null)service.driveRefund(refund);
    }
}
