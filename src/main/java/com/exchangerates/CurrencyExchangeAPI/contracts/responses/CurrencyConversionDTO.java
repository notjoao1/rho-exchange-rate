package com.exchangerates.CurrencyExchangeAPI.contracts.responses;

import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CurrencyConversionDTO {
    private String base;
    private Instant rateTimestamp;

    // target currencies and their values
    private Map<String, Double> targets;
}
