package com.citygo.config;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.beans.factory.annotation.Value;
@Component public class PaymentEnvironmentGuard {
    public PaymentEnvironmentGuard(Environment env,@Value("${citygo.payment.mock-enabled:true}")boolean mock){
        if(!mock)throw new IllegalStateException("This demo supports simulated payment only; mock-enabled must remain true");
        if(mock&&(env.acceptsProfiles(Profiles.of("prod"))||!env.acceptsProfiles(Profiles.of("dev","test"))))throw new IllegalStateException("Mock payment is allowed only in dev/test, never production");
    }
}
