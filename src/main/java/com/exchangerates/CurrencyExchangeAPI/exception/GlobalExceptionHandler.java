package com.exchangerates.CurrencyExchangeAPI.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.exchangerates.CurrencyExchangeAPI.contracts.responses.ErrorMessage;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorMessage> handleResponseStatusException(ResponseStatusException ex) {
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

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorMessage> handleMissingParameter(
            MissingServletRequestParameterException ex) {
        logger.error("Missing required parameter: {}", ex.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorMessage(ex.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorMessage> handleMissingResource(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorMessage("Not found"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorMessage> handleDefaultException(Exception ex) {
        logger.error("Exception caught - {}: {}", ex.getClass(), ex.getMessage());
        return ResponseEntity.status(500).body(new ErrorMessage("Unknown error occured"));
    }
}
