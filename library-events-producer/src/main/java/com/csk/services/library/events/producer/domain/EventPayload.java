package com.csk.services.library.events.producer.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record EventPayload (
        Integer eventId,
        EventType eventType,

        @NotNull
        @Valid
        Book book)
{ }
