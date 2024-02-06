package com.csk.services.library.events.consumer.processor;

import com.csk.services.library.events.consumer.repository.LibraryEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetryEventListener {

    private final LibraryEventService eventService;

    @KafkaListener(topics = {"${app.topics.retry}"},
    autoStartup = "${retry-listener.autostart:true}",
            groupId = "retry-event-listener")
    public void onMessage(ConsumerRecord<Integer, String> consumerRecord) {

        log.info("Retrying Failed Message: {}", consumerRecord);

        eventService.persistEvent(consumerRecord);
    }
}
