package com.csk.services.library.events.producer.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@RequiredArgsConstructor
public class AutoCreateConfig {

    private final AppConfig appConfig;

    @Bean
    public NewTopic libraryEvents() {

        return TopicBuilder.name(appConfig.getLibraryEventsTopicName())
                .partitions(3)
                .replicas(3)
                .build();
    }
}
