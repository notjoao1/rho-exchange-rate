package com.exchangerates.CurrencyExchangeAPI.domain;

import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CachedRates {
    private Map<String, Double> rates;
    private Instant timestamp;
}
