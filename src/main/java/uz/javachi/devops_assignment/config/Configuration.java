package uz.javachi.devops_assignment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;

import java.util.Random;

@Slf4j
@org.springframework.context.annotation.Configuration
public class Configuration {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean
    public MeterBinder customMetrics() {
        return (MeterRegistry registry) -> {
            // Application uptime metric
            Gauge.builder("application.uptime.seconds", System::currentTimeMillis)
                    .description("Application uptime in seconds")
                    .baseUnit("seconds")
                    .register(registry);

            // Custom business metric - simulated active sessions
            Random random = new Random();
            Gauge.builder("application.active.sessions", () -> random.nextInt(100))
                    .description("Number of active user sessions")
                    .tag("type", "http")
                    .register(registry);

            // Custom business metric - cache hit ratio
            Gauge.builder("application.cache.hit.ratio", () -> 0.75 + random.nextDouble() * 0.2)
                    .description("Cache hit ratio percentage")
                    .tag("cache", "main")
                    .register(registry);

            log.info("Custom metrics registered successfully");
        };
    }

}
