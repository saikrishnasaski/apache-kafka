package com.csk.services.library.events.consumer.config;

import lombok.Getter;
import lombok.Setter;
import org.apache.kafka.common.protocol.types.Field;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {

    private Topic topics;

    @Getter
    @Setter
    public static class Topic {

        private String libraryEvents;
        private String retry;
        private String dlt;
    }
}
