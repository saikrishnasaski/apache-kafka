package com.csk.services.library.events.producer.producer;

import com.csk.services.library.events.producer.config.AppConfig;
import com.csk.services.library.events.producer.domain.EventPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LibraryEventsProducer {

    private final AppConfig appConfig;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<Integer, String> kafkaTemplate;

    public void publishLibraryEvent(EventPayload eventPayload) {

        try {
            var key = eventPayload.eventId();
            var value = objectMapper.writeValueAsString(eventPayload);

            var producerRecord = buildProducerRecord(key, value);

            // 1. blocking call - get metadata about the kafka cluster (This happens only when sending message for the first time after application startup)
            // 2. async call - publish event returns a CompletableFuture
            var completableFuture = kafkaTemplate.send(producerRecord);

            completableFuture.whenComplete(((integerStringSendResult, throwable) -> {

                if (throwable != null) {

                    handleFailure(key, value, throwable);
                } else {

                    handleSuccess(key, value, integerStringSendResult);
                }
            }));

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private ProducerRecord<Integer, String> buildProducerRecord(Integer key, String value) {

        var topicName = appConfig.getLibraryEventsTopicName();
        List<Header> recordHeaders = List.of(new RecordHeader("event-source", "scanner".getBytes()));

        return new ProducerRecord<>(topicName, null, key, value, recordHeaders);
    }

    private void handleSuccess(Integer key, String value, SendResult<Integer, String> sendResult) {

        log.info("Message with key: {} \n value: {} \n to the partition: {}", key, value,
                sendResult.getRecordMetadata().partition());
    }

    private void handleFailure(Integer key, String value, Throwable throwable) {

        log.error("Message publishing with key: {} \n value: {} \n failed with exception: {}",
                key, value, throwable);
    }
}
