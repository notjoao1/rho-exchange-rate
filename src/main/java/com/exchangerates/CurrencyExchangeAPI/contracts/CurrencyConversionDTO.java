package com.exchangerates.CurrencyExchangeAPI.contracts;

import java.time.Instant;
import java.util.Map;
import lombok.Data;

@Data
public class CurrencyConversionDTO {
    private String base;
    private Instant rateTimestamp;

    // target currencies and their values
    private Map<String, Double> targets;
}
