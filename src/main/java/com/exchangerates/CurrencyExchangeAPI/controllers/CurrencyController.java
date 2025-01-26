package com.exchangerates.CurrencyExchangeAPI.controllers;

import com.exchangerates.CurrencyExchangeAPI.contracts.responses.CurrencyConversionDTO;
import com.exchangerates.CurrencyExchangeAPI.contracts.responses.ValueConversionDTO;
import com.exchangerates.CurrencyExchangeAPI.services.interfaces.ICurrencyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Arrays;
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
            @Valid @NotEmpty @RequestParam("from") String baseCurrency,
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
            @Valid @NotEmpty @RequestParam("from") String baseCurrency,
            @Valid @NotEmpty @RequestParam("to") String targetCurrencies,
            @Valid @Positive @NotNull @RequestParam("value") double valueToConvert) {
        // parse 'to' query parameter: a comma separated list of currencies encoded in
        // a single string. Example: 'USD,AUD,CAD,EUR,CHF' -> 'USD,AUD,CAD,EUR,CHF'
        var currenciesToConvertTo = targetCurrencies.toUpperCase().split(",");
        return ResponseEntity.ok(
                currencyService.convertCurrencyValues(
                        baseCurrency, Arrays.asList(currenciesToConvertTo), valueToConvert));
    }
}
