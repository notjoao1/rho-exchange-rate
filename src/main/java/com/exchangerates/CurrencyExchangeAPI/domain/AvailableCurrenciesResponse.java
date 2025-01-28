package com.exchangerates.CurrencyExchangeAPI.domain;

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
public class AvailableCurrenciesResponse {
    private boolean success;
    private Map<String, String> currencies;
    // filled out when success = false
    private ExternalAPIError error;
}
