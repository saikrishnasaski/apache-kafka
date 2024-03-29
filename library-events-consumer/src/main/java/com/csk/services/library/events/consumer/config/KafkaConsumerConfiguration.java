package com.csk.services.library.events.consumer.config;

import com.mongodb.MongoSocketOpenException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.ContainerCustomizer;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfiguration {

    private final AppConfig appConfig;
    private final KafkaTemplate kafkaTemplate;

    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer() {

        return new DeadLetterPublishingRecoverer(kafkaTemplate,
                ((consumerRecord, ex) -> {

                    if (ex.getCause() instanceof MongoSocketOpenException || ex.getCause() instanceof RecoverableDataAccessException) {

                        return new TopicPartition(appConfig.getTopics().getRetry(), consumerRecord.partition());

                    } else {

                        return new TopicPartition(appConfig.getTopics().getDlt(), consumerRecord.partition());
                    }
                }));
    }

    public DefaultErrorHandler errorHandler() {

        var notRetriableExceptions = List.of(IllegalArgumentException.class);
        var retriableExceptions = List.of(MongoSocketOpenException.class, RecoverableDataAccessException.class);
//        var fixedBackOff = new FixedBackOff(1000L, 2);
        var backOff = new ExponentialBackOff();
        backOff.setMaxAttempts(2);
        backOff.setInitialInterval(1000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(2000L);

        var errorHandler = new DefaultErrorHandler(deadLetterPublishingRecoverer(), backOff);

        notRetriableExceptions.forEach(errorHandler::addNotRetryableExceptions);
        retriableExceptions.forEach(errorHandler::addRetryableExceptions);

        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            log.info("Failed record in Retry Listener, Exception: {} \n deliveryAttempt: {}", ex.getMessage(), deliveryAttempt);
        });

        return errorHandler;
    }

    @Bean
    @ConditionalOnMissingBean(name = "kafkaListenerContainerFactory")
    ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ObjectProvider<ConsumerFactory<Object, Object>> kafkaConsumerFactory,
            ObjectProvider<ContainerCustomizer<Object, Object, ConcurrentMessageListenerContainer<Object, Object>>> kafkaContainerCustomizer,
            ObjectProvider<SslBundles> sslBundles) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, kafkaConsumerFactory.getIfAvailable());
        kafkaContainerCustomizer.ifAvailable(factory::setContainerCustomizer);

        factory.setConcurrency(3);
        factory.setCommonErrorHandler(errorHandler());

        //factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }
}
