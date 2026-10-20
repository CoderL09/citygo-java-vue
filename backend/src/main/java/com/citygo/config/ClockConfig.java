package com.citygo.config;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.*;
import java.time.*;
import java.nio.file.*;
/** Production uses wall time. A local test process may explicitly opt into a clock file; no HTTP time-changing endpoint exists. */
@Configuration public class ClockConfig {
 @Bean public Clock clock(Environment env,@Value("${citygo.test.clock-file:}")String file){if(file.isBlank())return Clock.systemUTC();
 if(env.acceptsProfiles(Profiles.of("prod"))||!env.acceptsProfiles(Profiles.of("dev","test")))throw new IllegalStateException("Test clock is allowed only in dev/test");
 if(env.getProperty("citygo.jobs.enabled",Boolean.class,true))throw new IllegalStateException("File clock requires jobs disabled to protect unrelated local orders");
 Path p=Path.of(file).toAbsolutePath().normalize();return new Clock(){public ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(ZoneId zone){if(!zone.equals(ZoneOffset.UTC))throw new IllegalArgumentException("UTC only");return this;}public Instant instant(){try{return Instant.parse(Files.readString(p).trim());}catch(Exception e){throw new IllegalStateException("Test clock file unreadable or invalid");}}};
 }
}
