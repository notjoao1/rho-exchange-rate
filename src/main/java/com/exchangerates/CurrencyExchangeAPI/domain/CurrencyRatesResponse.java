package com.exchangerates.CurrencyExchangeAPI.domain;

import java.time.Instant;
import java.util.Map;
import lombok.Data;

/**
 * Based on the Base API response from https://exchangerate.host/documentation
 */
@Data
public class CurrencyRatesResponse {
    private boolean success;
    private Instant timestamp;
    private String source;
    private Map<String, Double> quotes;
    // filled out when success = false
    private CurrencyRatesResponseError error;
}
