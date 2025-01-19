package com.exchangerates.CurrencyExchangeAPI;

import com.exchangerates.CurrencyExchangeAPI.contracts.CurrencyConversionDTO;
import com.exchangerates.CurrencyExchangeAPI.contracts.ValueConversionDTO;
import com.exchangerates.CurrencyExchangeAPI.interfaces.ICurrencyService;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@AllArgsConstructor
public class CurrencyService implements ICurrencyService {
    @Autowired private final RestTemplate httpClient;

    // the method that fetches external currency rates should handle caching
    // TODO: currency conversion from A to B
    // TODO: all exchange rates for currency A
    public CurrencyConversionDTO getCurrencyConversionRates(
            String baseCurrency, Optional<String> targetCurrency) {
        throw new UnsupportedOperationException();
    }

    // TODO: value conversion from currency A to B
    // TODO: value conversion from currency A to a list of currencies
    @Override
    public ValueConversionDTO convertCurrencyValues(
            String baseCurrency, Set<String> targetCurrencies, double valueToConvert) {
        throw new UnsupportedOperationException("Unimplemented method 'convertCurrencyValues'");
    }
}
