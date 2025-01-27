package com.exchangerates.CurrencyExchangeAPI.services.interfaces;

import java.util.List;

import com.exchangerates.CurrencyExchangeAPI.domain.AvailableCurrenciesResponse;
import com.exchangerates.CurrencyExchangeAPI.domain.CurrencyRatesResponse;

public interface ICurrencyAPIClient {
    AvailableCurrenciesResponse fetchAvailableCurrencies();
    CurrencyRatesResponse fetchCurrencyExchangeRates(String baseCurrency, List<String> targetCurrencies);
}
