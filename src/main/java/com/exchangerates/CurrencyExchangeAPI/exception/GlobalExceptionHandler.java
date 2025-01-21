package com.exchangerates.CurrencyExchangeAPI.exception;

import com.exchangerates.CurrencyExchangeAPI.contracts.ErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorMessage> handleRestTemplateException(ResponseStatusException ex) {
        var errorStatusCode = ex.getStatusCode();
        logger.error(
                "{} Error caught from HTTP client, with message: {}",
                errorStatusCode,
                ex.getMessage());

        String errorMessage;
        if (errorStatusCode == HttpStatus.NOT_FOUND) {
            errorMessage = "Currency not found.";
        } else {
            errorMessage = "An error occurred: " + ex.getMessage();
        }

        return ResponseEntity.status(errorStatusCode).body(new ErrorMessage(errorMessage));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessage> handleDefaultException(Exception ex) {
        logger.error("Exception caught - {}: {}", ex.getClass(), ex.getMessage());
        return ResponseEntity.status(500).body(new ErrorMessage("Unknown error occured"));
    }
}
