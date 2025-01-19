package com.exchangerates.CurrencyExchangeAPI.contracts;

import java.util.Map;
import lombok.Data;

@Data
public class ValueConversionDTO {

    private String base;
    private double requestedValue;

    // map between currencies converted to, and the associated converted value
    private Map<String, Double> conversions;
}
