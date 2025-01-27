package com.exchangerates.CurrencyExchangeAPI.domain;

import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Based on the Base API response from https://exchangerate.host/documentation
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CurrencyRatesResponse {
    private boolean success;
    private Instant timestamp;
    private String source;
    private Map<String, Double> quotes;
    // filled out when success = false
    private ExternalAPIError error;
}
