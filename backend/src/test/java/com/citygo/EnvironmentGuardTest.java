package com.citygo;
import com.citygo.config.ClockConfig;
import com.citygo.auth.service.AdminInitRunner;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import java.nio.file.*;
import java.time.*;
import static org.assertj.core.api.Assertions.*;
class EnvironmentGuardTest {
 @TempDir Path dir;
 @Test void clockFileIsExplicitAndRequiresJobsDisabled()throws Exception{Path f=dir.resolve("clock");Files.writeString(f,"2026-06-02T00:00:00Z");var e=new MockEnvironment();e.setActiveProfiles("dev");var config=new ClockConfig();assertThatThrownBy(()->config.clock(e,f.toString())).isInstanceOf(IllegalStateException.class);e.setProperty("citygo.jobs.enabled","false");var c=config.clock(e,f.toString());assertThat(c.instant()).isEqualTo(Instant.parse("2026-06-02T00:00:00Z"));Files.writeString(f,"2026-06-03T00:00:00Z");assertThat(c.instant()).isEqualTo(Instant.parse("2026-06-03T00:00:00Z"));e.setActiveProfiles("dev","prod");assertThatThrownBy(()->config.clock(e,f.toString())).isInstanceOf(IllegalStateException.class);}
 @Test void productionCannotLoadDevAdminBootstrap(){try(var c=new AnnotationConfigApplicationContext()){c.getEnvironment().setActiveProfiles("dev","prod");c.register(AdminInitRunner.class);c.refresh();assertThat(c.getBeansOfType(AdminInitRunner.class)).isEmpty();}}
}
