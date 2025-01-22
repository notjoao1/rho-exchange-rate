package com.exchangerates.CurrencyExchangeAPI.services;

import com.exchangerates.CurrencyExchangeAPI.contracts.CurrencyConversionDTO;
import com.exchangerates.CurrencyExchangeAPI.contracts.ValueConversionDTO;
import com.exchangerates.CurrencyExchangeAPI.domain.CurrencyRatesResponse;
import com.exchangerates.CurrencyExchangeAPI.services.interfaces.ICacheService;
import com.exchangerates.CurrencyExchangeAPI.services.interfaces.ICurrencyService;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class CurrencyService implements ICurrencyService {
    private static final Logger logger = LoggerFactory.getLogger(CurrencyService.class);
    private final RestTemplate httpClient;
    private final ICacheService<CurrencyRatesResponse> cacheService;
    private final String BASE_FRANKFURTER_API_URL = "https://api.frankfurter.dev/v1";

    // default rates TTL to 60 seconds
    @Value("${cache.ttl.rates:60}")
    private long ratesTtlSeconds;

    @Autowired
    public CurrencyService(RestTemplate httpClient, ICacheService<CurrencyRatesResponse> cacheService) {
        this.httpClient = httpClient;
        this.cacheService = cacheService;
    }

    public CurrencyConversionDTO getCurrencyConversionRates(
            String baseCurrency, Optional<String> targetCurrency) {

        var cachedCurrencyRates = getCachedRatesResponse(baseCurrency, targetCurrency);
        if (cachedCurrencyRates.isPresent()) {
            logger.debug("Cache HIT for base = '{}', target = '{}'.", baseCurrency, targetCurrency);
            var currencyRate = cachedCurrencyRates.get();
            return mapToConversionDTO(currencyRate);
        }
        logger.debug("Cache MISS for base = '{}', target = '{}'.", baseCurrency, targetCurrency);

        var requestUri =
                UriComponentsBuilder.fromUriString(BASE_FRANKFURTER_API_URL + "/latest")
                        .queryParam("base", baseCurrency)
                        .queryParamIfPresent("symbols", targetCurrency)
                        .toUriString();

        logger.info("Making a GET request to external API with URI = '{}'.", requestUri);
        var currencyRatesResponse =
                httpClient.getForEntity(requestUri, CurrencyRatesResponse.class)
                          .getBody();
        currencyRatesResponse.setTimestamp(Instant.now());

        // cache the response for the defined TTL
        var requestCacheKey = buildCacheKey(baseCurrency, targetCurrency);
        logger.info("Caching API response for key '{}''.", requestCacheKey);
        cacheService.set(requestCacheKey, currencyRatesResponse, Duration.ofSeconds(ratesTtlSeconds));

        return mapToConversionDTO(currencyRatesResponse);
    }

    // TODO: value conversion from currency A to B
    // TODO: value conversion from currency A to a list of currencies
    @Override
    public ValueConversionDTO convertCurrencyValues(
            String baseCurrency, Set<String> targetCurrencies, double valueToConvert) {
        var requestUri =
                UriComponentsBuilder.fromUriString(BASE_FRANKFURTER_API_URL + "/latest")
                        .queryParam("base", baseCurrency)
                        .queryParam(
                                "symbols",
                                targetCurrencies.stream()
                                        .reduce(
                                                "",
                                                (symbolsString, currency) ->
                                                        String.format(
                                                                "%s,%s", symbolsString, currency)))
                        .toUriString();

        logger.info("Making a GET request to external API with URI = '{}'.", requestUri);
        var currencyRatesResponse =
                httpClient.getForEntity(requestUri, CurrencyRatesResponse.class);

        // build a response conversion DTO
        var valueConversionResponse =
                new ValueConversionDTO(baseCurrency, valueToConvert, new HashMap<>());

        for (var currencyExchangeRatePair : currencyRatesResponse.getBody().getRates().entrySet()) {
            String targetCurrency = currencyExchangeRatePair.getKey();
            double conversionRate = currencyExchangeRatePair.getValue();
            valueConversionResponse
                    .getConversions()
                    .put(targetCurrency, valueToConvert * conversionRate);
        }

        return valueConversionResponse;
    }

    /**
     * Retrieves the rates response from the cache if present. If targetCurrency is included,
     * searches for cached response from (baseCurrency) -> (targetCurrency), but also
     * the other way around.
     * @param baseCurrency The base currency to search for.
     * @param targetCurrency The target currency to search for.
     * @return An optional containing a cached exchange rate response, if present in the cache.
     */
    private Optional<CurrencyRatesResponse> getCachedRatesResponse(String baseCurrency, Optional<String> targetCurrency) {
        if (targetCurrency.isEmpty()) {
            return cacheService.get(buildCacheKey(baseCurrency, targetCurrency));
        }

        var baseToTarget = cacheService.get(buildCacheKey(baseCurrency, targetCurrency));    
        if (baseToTarget.isPresent()) {
            return baseToTarget;
        }

        var targetToBase = cacheService.get(buildCacheKey(targetCurrency.get(), Optional.of(baseCurrency)));
        if (targetToBase.isPresent()) {
            var targetToBaseCachedRate = targetToBase.get();
            // we have B -> A, but we have to return A -> B, so that it is transparent
            // for the consumer of this method
            var reversedRatesResponse = new CurrencyRatesResponse();
            reversedRatesResponse.setBase(baseCurrency);
            reversedRatesResponse.setTimestamp(targetToBaseCachedRate.getTimestamp());

            var reversedConversionRate = targetToBaseCachedRate.getRates().get(baseCurrency);
            // A -> B rate is equal to 1/(B -> A) rate
            reversedRatesResponse.setRates(Map.of(targetCurrency.get(), 1/reversedConversionRate));
            return Optional.of(reversedRatesResponse);
        }

        // if we have a mapping of target -> (ALL), we can also infer base -> target
        var targetToAny = cacheService.get(buildCacheKey(targetCurrency.get(), Optional.empty()));
        if (targetToAny.isPresent()) {
            var targetToAnyCachedRates = targetToAny.get();

            var reversedRatesResponse = new CurrencyRatesResponse();
            reversedRatesResponse.setBase(baseCurrency);
            reversedRatesResponse.setTimestamp(targetToAnyCachedRates.getTimestamp());

            var reversedConversionRate = targetToAnyCachedRates.getRates().get(baseCurrency);
            // A -> B rate is equal to 1/(B -> A) rate
            reversedRatesResponse.setRates(Map.of(targetCurrency.get(), 1/reversedConversionRate));
            return Optional.of(reversedRatesResponse);
        }

        return Optional.empty();
    }


    /**
     * Builds a cache key for caching responses based on baseCurrency and targetCurrency
     * @return If target currency is present, returns 'rates:baseCurrency:targetCurrency'.
     * Otherwise, 'rates:baseCurrency'
     */
    private String buildCacheKey(String baseCurrency, Optional<String> targetCurrency) {
        return targetCurrency.isPresent()
                ? String.format("rates:%s:%s", baseCurrency, targetCurrency.get())
                : String.format("rates:%s", baseCurrency);
    }

    private CurrencyConversionDTO mapToConversionDTO(CurrencyRatesResponse res) {
        return new CurrencyConversionDTO(res.getBase(), res.getTimestamp(), res.getRates());
    }
}
