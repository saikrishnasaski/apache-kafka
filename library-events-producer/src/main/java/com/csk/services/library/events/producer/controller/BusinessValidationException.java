package com.csk.services.library.events.producer.controller;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BusinessValidationException extends RuntimeException {

    private String target;
    public BusinessValidationException(String invalidEventTypeForUpdate, String target) {
        super(invalidEventTypeForUpdate);

        this.target = target;
    }
}
