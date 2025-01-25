package com.exchangerates.CurrencyExchangeAPI.domain;

import lombok.Data;

@Data
public class CurrencyRatesResponseError {
    private int code;
    private String info;
}
