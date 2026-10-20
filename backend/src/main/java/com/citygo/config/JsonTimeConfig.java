package com.citygo.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

/** MySQL DATETIME stores UTC without an offset; the API always makes UTC explicit. */
@Configuration
public class JsonTimeConfig {
    @Bean
    Jackson2ObjectMapperBuilderCustomizer utcDateTimes() {
        return builder -> {
            builder.serializerByType(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
                @Override public void serialize(LocalDateTime value, JsonGenerator gen,
                        SerializerProvider provider) throws IOException {
                    gen.writeString(value.atOffset(ZoneOffset.UTC).toString());
                }
            });
            builder.deserializerByType(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
                @Override public LocalDateTime deserialize(JsonParser p,
                        DeserializationContext context) throws IOException {
                    try {
                        return OffsetDateTime.parse(p.getText()).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
                    } catch (DateTimeParseException e) {
                        return (LocalDateTime) context.handleWeirdStringValue(LocalDateTime.class,
                                p.getText(), "ISO timestamp must include Z or an explicit offset");
                    }
                }
            });
        };
    }
}
