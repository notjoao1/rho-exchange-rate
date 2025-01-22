package com.exchangerates.CurrencyExchangeAPI.domain;

import java.time.Instant;
import java.util.Map;
import lombok.Data;

@Data
public class CachedRates {
    private final Map<String, Double> rates;
    private final Instant timestamp;
}
