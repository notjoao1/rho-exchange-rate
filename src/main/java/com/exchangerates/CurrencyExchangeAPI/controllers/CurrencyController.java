package com.exchangerates.CurrencyExchangeAPI.controllers;

import com.exchangerates.CurrencyExchangeAPI.contracts.CurrencyConversionDTO;
import com.exchangerates.CurrencyExchangeAPI.contracts.ValueConversionDTO;
import com.exchangerates.CurrencyExchangeAPI.services.interfaces.ICurrencyService;
import java.util.ArrayList;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/currency")
@AllArgsConstructor
public class CurrencyController {
    private final ICurrencyService currencyService;

    /**
     * Fetches and returns currency conversion rates between different currencies.
     * @param baseCurrency - the base currency to fetch conversion rates from
     * @param targetCurrency - optionally, the target currency to get conversion rates to. If not supplied, returns conversion rates from *baseCurrency*
     * to all other available currencies.
     */
    @GetMapping("rate")
    public ResponseEntity<CurrencyConversionDTO> getConversionRateBetweenCurrencies(
            @RequestParam("from") String baseCurrency,
            @RequestParam("to") Optional<String> targetCurrency) {
        return ResponseEntity.ok(
                currencyService.getCurrencyConversionRates(baseCurrency, targetCurrency));
    }

    /**
     * Performs currency conversion from a specific currency to another, given a currency amount to convert.
     * @param baseCurrency - the base currency to convert the value from
     * @param targetCurrencies - the target currencies to convert the value to
     */
    @GetMapping("convert")
    public ResponseEntity<ValueConversionDTO> getConversionRateBetweenCurrencies(
            @RequestParam("from") String baseCurrency,
            @RequestParam("to") String targetCurrencies,
            @RequestParam("value") double valueToConvert) {
        // parse 'to' query parameter: a comma separated list of currencies encoded in
        // a single string. Example: 'USD,AUD,CAD,EUR,CHF' -> 'USD,AUD,CAD,EUR,CHF'
        var targetCurrencyList = new ArrayList<String>();
        var currenciesToConvertTo = targetCurrencies.toUpperCase().split(",");
        for (var currency : currenciesToConvertTo) {
            targetCurrencyList.add(currency);
        }

        return ResponseEntity.ok(
                currencyService.convertCurrencyValues(
                        baseCurrency, targetCurrencyList, valueToConvert));
    }
}
