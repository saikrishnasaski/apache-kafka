package com.csk.services.library.events.consumer.processor;

import com.csk.services.library.events.consumer.repository.LibraryEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventListener {

    private final LibraryEventService eventService;

    @KafkaListener(topics = {"library-events"})
    public void onMessage(ConsumerRecord<Integer, String> consumerRecord) throws IOException {

        log.info("Received Message: {}", consumerRecord);

        eventService.persistEvent(consumerRecord);
    }
}
