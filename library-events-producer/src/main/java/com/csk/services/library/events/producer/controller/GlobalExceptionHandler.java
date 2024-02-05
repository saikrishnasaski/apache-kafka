package com.csk.services.library.events.producer.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<List<Error>> handleBusinessValidationException(BusinessValidationException ex) {

        var error = new Error(BAD_REQUEST.getReasonPhrase(), ex.getMessage(), ex.getTarget());

        return ResponseEntity.badRequest().body(List.of(error));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<List<Error>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {

        var errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> new Error(BAD_REQUEST.getReasonPhrase(), fieldError.getDefaultMessage(), fieldError.getField()))
                .toList();
        
        return ResponseEntity.badRequest().body(errors);
    }

    record Error(String errorCode, String errorMessage, String target) {}
}
