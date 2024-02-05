package com.csk.services.library.events.consumer.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static com.csk.services.library.events.consumer.repository.EventPayload.SEQUENCE_NAME;

@Slf4j
@Service
@RequiredArgsConstructor
public class LibraryEventService {

    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;
    private final SequenceGeneratorService sequenceGeneratorService;

    public void persistEvent(ConsumerRecord<Integer, String> consumerRecord) throws IOException {

        var eventPayload = consumerRecord.value();

        var libraryEventPayload = objectMapper.readValue(eventPayload.getBytes(), EventPayload.class);

        if (EventType.UPDATE == libraryEventPayload.eventType) {

            if (libraryEventPayload.eventId == null) {

                throw new IllegalArgumentException("EventId is null");
            }

            var libaryEventUpdateRecord = mongoTemplate.findById(libraryEventPayload.eventId, EventPayload.class);

            if (libaryEventUpdateRecord == null) {

                throw new IllegalArgumentException("Invalid Library Event");
            }
        }
        else {

            var eventId = sequenceGeneratorService.generateSequence(SEQUENCE_NAME);
            libraryEventPayload.setEventId(eventId);
        }

        mongoTemplate.save(libraryEventPayload);
    }
}
