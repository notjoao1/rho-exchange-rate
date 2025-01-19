package com.exchangerates.CurrencyExchangeAPI;

import com.exchangerates.CurrencyExchangeAPI.contracts.CurrencyConversionDTO;
import com.exchangerates.CurrencyExchangeAPI.contracts.ValueConversionDTO;
import java.time.Instant;
import java.util.Map;
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
    private final CurrencyService currencyService;

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
        var test = new CurrencyConversionDTO();
        test.setBase("EUR");
        test.setRateTimestamp(Instant.now());
        test.setTargets(Map.of("USD", 1.2, "GBP", 0.8, "JPY", 130.0));
        return ResponseEntity.ok(test);
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

        var test = new ValueConversionDTO();
        test.setBase("EUR");
        test.setRequestedValue(20.0);
        test.setConversions(Map.of("USD", 21.25, "CAD", 25.0));
        return ResponseEntity.ok(test);
    }
}
