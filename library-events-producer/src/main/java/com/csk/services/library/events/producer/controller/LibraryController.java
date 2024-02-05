package com.csk.services.library.events.producer.controller;

import com.csk.services.library.events.producer.domain.EventPayload;
import com.csk.services.library.events.producer.domain.EventType;
import com.csk.services.library.events.producer.domain.UpdateLibraryEvent;
import com.csk.services.library.events.producer.producer.LibraryEventsProducer;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class LibraryController {

    private final LibraryEventsProducer eventProducer;

    @PostMapping("/libraryevent")
    public ResponseEntity<EventPayload> createLibraryEvent(@RequestBody @Valid EventPayload eventPayload) {

        log.info("Producing LibraryEvent {}", eventPayload);

        eventProducer.publishLibraryEvent(eventPayload);

        log.info("Message Published");

        return ResponseEntity.status(HttpStatus.CREATED).body(eventPayload);
    }

    @PutMapping("/libraryevent")
    public ResponseEntity<EventPayload> updateLibraryEvent(
            @RequestBody @Validated(UpdateLibraryEvent.class) EventPayload eventPayload) {

        if (eventPayload.eventType() != EventType.UPDATE) {

            throw new BusinessValidationException("Invalid Event Type for Update", "eventType");
        }

        log.info("Producing LibraryEvent {}", eventPayload);

        eventProducer.publishLibraryEvent(eventPayload);

        log.info("Message Published");

        return ResponseEntity.status(HttpStatus.CREATED).body(eventPayload);
    }
}
