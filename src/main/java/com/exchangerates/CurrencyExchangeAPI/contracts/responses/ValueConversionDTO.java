package com.exchangerates.CurrencyExchangeAPI.contracts.responses;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ValueConversionDTO {

    private String base;
    private double requestedValue;

    // map between currencies converted to, and the associated converted value
    private Map<String, Double> conversions;
}
