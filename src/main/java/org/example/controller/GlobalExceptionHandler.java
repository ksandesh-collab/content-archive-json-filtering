package org.example.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<PublicationController.ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new PublicationController.ErrorResponse("Missing required parameter: " + ex.getParameterName()));
    }
}
