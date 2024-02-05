package com.csk.services.library.events.producer.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record EventPayload (

        @NotNull(groups = UpdateLibraryEvent.class)
        Integer eventId,
        EventType eventType,

        @NotNull
        @Valid
        Book book)
{ }
