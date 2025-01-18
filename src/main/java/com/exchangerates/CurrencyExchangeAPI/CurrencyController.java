package com.exchangerates.CurrencyExchangeAPI;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller("/api/v1/currency")
public class CurrencyController {
    /**
     * Fetches and returns currency conversion rates between different currencies.
     * @param baseCurrency - the base currency to fetch conversion rates from
     * @param targetCurrency - optionally, the target currency to get conversion rates to. If not supplied, returns conversion rates from *baseCurrency* 
     * to all other available currencies.
     * @return TODO:
     */
    @GetMapping("rate")
    public ResponseEntity<Void> getConversionRateBetweenCurrencies(@RequestParam("from") String baseCurrency, @RequestParam("to") Optional<String> targetCurrency) {
        return ResponseEntity.ok().build();
    }

    /**
     * Performs currency conversion from a specific currency to another, given a currency amount to convert.
     * @param baseCurrency - the base currency to convert the value from
     * @param targetCurrencies - the target currencies to convert the value to
     * @return TODO:
     */
    @GetMapping("convert")
    public ResponseEntity<Void> getConversionRateBetweenCurrencies(
        @RequestParam("from") String baseCurrency, 
        @RequestParam("to") String targetCurrencies, 
        @RequestParam("value") double valueToConvert) {

            //TODO: this also has to fetch conversion rates
            return ResponseEntity.ok().build();
    }
}
