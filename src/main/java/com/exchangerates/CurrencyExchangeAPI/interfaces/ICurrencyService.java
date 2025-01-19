package com.exchangerates.CurrencyExchangeAPI.interfaces;

import com.exchangerates.CurrencyExchangeAPI.contracts.CurrencyConversionDTO;
import com.exchangerates.CurrencyExchangeAPI.contracts.ValueConversionDTO;
import java.util.Optional;
import java.util.Set;

public interface ICurrencyService {
    /**
     * Fetches and returns currency conversion rates between different currencies.
     * @param baseCurrency - the base currency to fetch conversion rates from
     * @param targetCurrency - optionally, the target currency to get conversion rates to. If not supplied, returns conversion
     * rates from *baseCurrency* to all other available currencies.
     * @return CurrencyConversionDTO - a DTO containing the base currency, the timestamp of the rates, and a map of target currencies
     * and their respective conversion rates.
     */
    CurrencyConversionDTO getCurrencyConversionRates(
            String baseCurrency, Optional<String> targetCurrency);

    /**
     * Performs currency conversion from a specific currency to another, given a currency amount to convert.
     * @param baseCurrency - the base currency to convert the value from
     * @param targetCurrencies - the target currencies to convert the value to
     * @param valueToConvert - the value to convert
     * @return ValueConversionDTO - a DTO containing the base currency, the value to convert, and a map of target currencies and their
     * respective converted values.
     */
    ValueConversionDTO convertCurrencyValues(
            String baseCurrency, Set<String> targetCurrencies, double valueToConvert);
}
