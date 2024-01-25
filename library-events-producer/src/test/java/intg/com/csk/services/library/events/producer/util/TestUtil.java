package com.csk.services.library.events.producer.util;

import com.csk.services.library.events.producer.domain.Book;
import com.csk.services.library.events.producer.domain.EventPayload;
import com.csk.services.library.events.producer.domain.EventType;

public class TestUtil {

    public static EventPayload eventPayload() {

        return new EventPayload(121, EventType.NEW, book());
    }

    public static Book book() {

        return new Book(12, "Haraveer Reddy",
                "AstroPhysics");
    }

    public static EventPayload invalidEventPayload() {

        return new EventPayload(121, EventType.NEW, bookRecordWithInvalidValues());
    }

    public static Book bookRecordWithInvalidValues() {

        return new Book(null, "Haraveer Reddy",
                "AstroPhysics");
    }
}
