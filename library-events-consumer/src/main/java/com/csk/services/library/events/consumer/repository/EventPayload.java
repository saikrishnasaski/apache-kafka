package com.csk.services.library.events.consumer.repository;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document("library-event")
class EventPayload {

        @Transient
        public static final String SEQUENCE_NAME = "eventid_sequence";

        @Id
        Integer eventId;

        EventType eventType;

        Book book;
}
