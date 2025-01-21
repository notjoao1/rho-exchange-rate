package com.exchangerates.CurrencyExchangeAPI.domain;

import java.util.Map;
import lombok.Data;

@Data
public class CurrencyRatesResponse {
    private String base;
    // rates to convert between 'base' and other currencies
    private Map<String, Double> rates;
}
