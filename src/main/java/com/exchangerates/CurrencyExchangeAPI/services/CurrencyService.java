package com.exchangerates.CurrencyExchangeAPI.services;

import com.exchangerates.CurrencyExchangeAPI.contracts.CurrencyConversionDTO;
import com.exchangerates.CurrencyExchangeAPI.contracts.ValueConversionDTO;
import com.exchangerates.CurrencyExchangeAPI.domain.CachedRates;
import com.exchangerates.CurrencyExchangeAPI.domain.CurrencyRatesResponse;
import com.exchangerates.CurrencyExchangeAPI.services.interfaces.ICacheService;
import com.exchangerates.CurrencyExchangeAPI.services.interfaces.ICurrencyService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final ICacheService<CachedRates> cacheService;
    private final String BASE_EXCHANGERATE_API_URL = "https://api.exchangerate.host/live";

    // default rates TTL to 60 seconds
    @Value("${cache.ttl.rates:60}")
    private long ratesTtlSeconds;

    @Value("${currency.exchangerate.apikey}")
    private String exchangeRateKey;

    @Autowired
    public CurrencyService(RestTemplate httpClient, ICacheService<CachedRates> cacheService) {
        this.httpClient = httpClient;
        this.cacheService = cacheService;
    }

    @Override
    public CurrencyConversionDTO getCurrencyConversionRates(
            String baseCurrency, Optional<String> targetCurrency) {

        var currencyRatesResponse =
                (targetCurrency.isEmpty())
                        ? fetchCurrencyExchangeRates(baseCurrency, List.of())
                        : fetchCurrencyExchangeRates(baseCurrency, List.of(targetCurrency.get()));

        return mapToConversionDTO(currencyRatesResponse);
    }

    @Override
    public ValueConversionDTO convertCurrencyValues(
            String baseCurrency, List<String> targetCurrencies, double valueToConvert) {
        var currencyRatesResponse = fetchCurrencyExchangeRates(baseCurrency, targetCurrencies);

        // build a response conversion DTO
        var valueConversionResponse =
                new ValueConversionDTO(baseCurrency, valueToConvert, new HashMap<>());

        for (var currencyExchangeRatePair : currencyRatesResponse.getQuotes().entrySet()) {
            String targetCurrency = currencyExchangeRatePair.getKey();
            double conversionRate = currencyExchangeRatePair.getValue();
            valueConversionResponse
                    .getConversions()
                    .put(targetCurrency, valueToConvert * conversionRate);
        }

        return valueConversionResponse;
    }

    /**
     * Inner method to fetch exchange rates, handling caching and multiple currencies.
     * @return Returns the external API's response to the query of currency exchange rates
     */
    private CurrencyRatesResponse fetchCurrencyExchangeRates(
            String baseCurrency, List<String> targetCurrencies) {
        // Check for caching: only when we only have a single targetCurrency, or 0 targetCurrencies
        if (targetCurrencies.size() <= 1) {
            // check for cached exchange rates
            Optional<String> targetCurrency = targetCurrencies.stream().findFirst();
            var cachedCurrencyRates = getCachedRatesResponse(baseCurrency, targetCurrency);

            if (cachedCurrencyRates.isPresent()) {
                logger.debug(
                        "Cache HIT for base = '{}', target = '{}'.", baseCurrency, targetCurrency);
                var responseToReturn = new CurrencyRatesResponse();
                responseToReturn.setQuotes(cachedCurrencyRates.get().getRates());
                responseToReturn.setSource(baseCurrency);
                responseToReturn.setTimestamp(cachedCurrencyRates.get().getTimestamp());

                return responseToReturn;
            }
            logger.debug("Cache MISS for base = '{}', target = '{}'", baseCurrency, targetCurrency);
        }

        // example: query from USD to EUR/CHF/SGD -
        // {baseURL}/live?accessKey=X&source=USD&currencies=EUR,CHF,SGD
        var requestUri =
                UriComponentsBuilder.fromUriString(BASE_EXCHANGERATE_API_URL)
                        .queryParam("source", baseCurrency)
                        .queryParam(
                                "currencies",
                                targetCurrencies.stream()
                                        .reduce(
                                                "",
                                                (symbolsString, currency) ->
                                                        String.format(
                                                                "%s,%s", symbolsString, currency)))
                        .toUriString();

        var currencyRatesResponse =
                httpClient.getForEntity(requestUri, CurrencyRatesResponse.class).getBody();
        // save fetched rates to cache
        saveRatesResponseToCache(currencyRatesResponse);

        return currencyRatesResponse;
    }

    /**
     * Retrieves the rates response from the cache if present. If targetCurrency is included,
     * searches for cached response from (baseCurrency) -> (targetCurrency), but also
     * the other way around.
     * @param baseCurrency The base currency to search for.
     * @param targetCurrency The target currency to search for.
     * @return An optional containing a cached exchange rate response, if present in the cache.
     */
    private Optional<CachedRates> getCachedRatesResponse(
            String baseCurrency, Optional<String> targetCurrency) {
        if (targetCurrency.isEmpty()) {
            return cacheService.get(buildCacheKey(baseCurrency, targetCurrency));
        }

        // A -> B cache look up
        var baseToTarget = cacheService.get(buildCacheKey(baseCurrency, targetCurrency));
        if (baseToTarget.isPresent()) {
            return baseToTarget;
        }

        // B -> A cache look up
        var targetToBase =
                cacheService.get(buildCacheKey(targetCurrency.get(), Optional.of(baseCurrency)));
        if (targetToBase.isPresent()) {
            var targetToBaseCachedRate = targetToBase.get();
            var reversedConversionRate = targetToBaseCachedRate.getRates().get(baseCurrency);
            // A -> B rate ==>  1/(B -> A) rate
            return Optional.of(
                    new CachedRates(
                            Map.of(targetCurrency.get(), 1 / reversedConversionRate),
                            targetToBaseCachedRate.getTimestamp()));
        }

        // B -> (ALL) cache look up
        var targetToAny = cacheService.get(buildCacheKey(targetCurrency.get(), Optional.empty()));
        if (targetToAny.isPresent()) {
            var targetToAnyCachedRates = targetToAny.get();
            var reversedConversionRate = targetToAnyCachedRates.getRates().get(baseCurrency);
            // A -> B rate is equal to 1/(B -> A) rate
            return Optional.of(
                    new CachedRates(
                            Map.of(targetCurrency.get(), 1 / reversedConversionRate),
                            targetToAnyCachedRates.getTimestamp()));
        }

        // A -> (ALL) cache look up
        var baseToAny = cacheService.get(buildCacheKey(baseCurrency, Optional.empty()));
        if (baseToAny.isPresent()) {
            var baseToAnyCachedRates = baseToAny.get();
            var conversionRate = baseToAnyCachedRates.getRates().get(targetCurrency.get());
            // A -> B rate is equal to 1/(B -> A) rate
            return Optional.of(
                    new CachedRates(
                            Map.of(targetCurrency.get(), conversionRate),
                            baseToAnyCachedRates.getTimestamp()));
        }

        return Optional.empty();
    }

    private void saveRatesResponseToCache(CurrencyRatesResponse res) {
        for (var currencyExchangePair : res.getQuotes().entrySet()) {
            var target = currencyExchangePair.getKey();
            cacheService.set(
                    buildCacheKey(res.getSource(), Optional.of(target)),
                    new CachedRates(
                            Map.of(target, currencyExchangePair.getValue()), res.getTimestamp()));
        }
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
        return new CurrencyConversionDTO(res.getSource(), res.getTimestamp(), res.getQuotes());
    }
}
