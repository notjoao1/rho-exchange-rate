package com.exchangerates.CurrencyExchangeAPI.services.interfaces;

import com.exchangerates.CurrencyExchangeAPI.domain.AvailableCurrenciesResponse;
import com.exchangerates.CurrencyExchangeAPI.domain.CurrencyRatesResponse;
import java.util.List;

public interface ICurrencyAPIClient {
    AvailableCurrenciesResponse fetchAvailableCurrencies();

    CurrencyRatesResponse fetchCurrencyExchangeRates(
            String baseCurrency, List<String> targetCurrencies);
}
