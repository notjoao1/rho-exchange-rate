package com.exchangerates.CurrencyExchangeAPI.services;

import com.exchangerates.CurrencyExchangeAPI.contracts.CurrencyConversionDTO;
import com.exchangerates.CurrencyExchangeAPI.contracts.ValueConversionDTO;
import com.exchangerates.CurrencyExchangeAPI.domain.CurrencyRatesResponse;
import com.exchangerates.CurrencyExchangeAPI.services.interfaces.ICurrencyService;

import java.time.Instant;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@AllArgsConstructor
public class CurrencyService implements ICurrencyService {
    @Autowired private final RestTemplate httpClient;

    private static final Logger logger = LoggerFactory.getLogger(CurrencyService.class);

    private final String BASE_FRANKFURTER_API_URL = "https://api.frankfurter.dev/v1";

    // TODO: the method that fetches external currency rates should handle caching
    public CurrencyConversionDTO getCurrencyConversionRates(
            String baseCurrency, Optional<String> targetCurrency) {
        var requestUri = UriComponentsBuilder.fromUriString(BASE_FRANKFURTER_API_URL + "/latest")
                .queryParam("base", baseCurrency)
                .queryParamIfPresent("symbols", targetCurrency)
                .toUriString();

        logger.info("Making a GET request to external API with URI = '{}'.", requestUri);

        // execute request
        // TODO: what if it fails and returns 404? does this throw?
        var currencyRatesResponse = httpClient.getForEntity(requestUri, CurrencyRatesResponse.class);

        return new CurrencyConversionDTO(baseCurrency, Instant.now(), currencyRatesResponse.getBody().getRates());
    }

    // TODO: value conversion from currency A to B
    // TODO: value conversion from currency A to a list of currencies
    @Override
    public ValueConversionDTO convertCurrencyValues(
            String baseCurrency, Set<String> targetCurrencies, double valueToConvert) {
        var requestUri = UriComponentsBuilder.fromUriString(BASE_FRANKFURTER_API_URL + "/latest")
                .queryParam("base", baseCurrency)
                .queryParam("symbols", targetCurrencies
                    .stream()
                    .reduce("", 
                        (symbolsString, currency) -> String.format("%s,%s", symbolsString, currency)))
                .toUriString();

        var currencyRatesResponse = httpClient.getForEntity(requestUri, CurrencyRatesResponse.class);

        // build a response conversion DTO
        var valueConversionResponse = new ValueConversionDTO(baseCurrency, valueToConvert, new HashMap<>());
        for (var currencyExchangeRatePair : currencyRatesResponse.getBody().getRates().entrySet()) {
            String targetCurrency = currencyExchangeRatePair.getKey();
            double conversionRate = currencyExchangeRatePair.getValue();
            valueConversionResponse.getConversions().put(targetCurrency, valueToConvert * conversionRate);
        }

        return valueConversionResponse;
    }
}
