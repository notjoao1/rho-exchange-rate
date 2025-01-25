package com.exchangerates.CurrencyExchangeAPI.exception;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
