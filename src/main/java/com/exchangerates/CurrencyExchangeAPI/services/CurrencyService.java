package com.exchangerates.CurrencyExchangeAPI.services;

import com.exchangerates.CurrencyExchangeAPI.contracts.responses.CurrencyConversionDTO;
import com.exchangerates.CurrencyExchangeAPI.contracts.responses.ValueConversionDTO;
import com.exchangerates.CurrencyExchangeAPI.domain.CachedRates;
import com.exchangerates.CurrencyExchangeAPI.domain.CurrencyRatesResponse;
import com.exchangerates.CurrencyExchangeAPI.exception.BusinessException;
import com.exchangerates.CurrencyExchangeAPI.services.interfaces.ICacheKeyBuilderService;
import com.exchangerates.CurrencyExchangeAPI.services.interfaces.ICacheService;
import com.exchangerates.CurrencyExchangeAPI.services.interfaces.ICurrencyAPIClient;
import com.exchangerates.CurrencyExchangeAPI.services.interfaces.ICurrencyService;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CurrencyService implements ICurrencyService {
    private static final Logger logger = LoggerFactory.getLogger(CurrencyService.class);
    private final ICurrencyAPIClient currencyAPIClient;
    private final ICacheService<CachedRates> cacheService;
    private final ICacheKeyBuilderService cacheKeyBuilderService;
    private final AvailableCurrenciesHolder existingCurrencies;

    // default rates TTL to 60 seconds
    @Value("${cache.ttl.rates:60}")
    private long ratesTtlSeconds;

    @Autowired
    public CurrencyService(
            ICurrencyAPIClient currencyAPIClient,
            ICacheService<CachedRates> cacheService,
            ICacheKeyBuilderService cacheKeyBuilderService,
            AvailableCurrenciesHolder existingCurrencies) {
        this.currencyAPIClient = currencyAPIClient;
        this.cacheService = cacheService;
        this.cacheKeyBuilderService = cacheKeyBuilderService;
        this.existingCurrencies = existingCurrencies;
    }

    @Override
    public CurrencyConversionDTO getCurrencyConversionRates(
            String baseCurrency, Optional<String> targetCurrency) {
        if (targetCurrency.isPresent() && baseCurrency == targetCurrency.get()) {
            throw new BusinessException("Base currency and target currency cannot be the same.");
        }

        if (!existingCurrencies.doesCurrencyExist(baseCurrency)) {
            throw new BusinessException("Invalid base currency - does not exist");
        }

        if (targetCurrency.isPresent()
                && !existingCurrencies.doesCurrencyExist(targetCurrency.get())) {
            throw new BusinessException("Invalid target currency - does not exist");
        }

        var currencyRatesResponse =
                (targetCurrency.isEmpty())
                        ? fetchCurrencyExchangeRates(baseCurrency, List.of())
                        : fetchCurrencyExchangeRates(baseCurrency, List.of(targetCurrency.get()));

        return mapToConversionDTO(currencyRatesResponse);
    }

    @Override
    public ValueConversionDTO convertCurrencyValues(
            String baseCurrency, List<String> targetCurrencies, double valueToConvert) {
        if (valueToConvert <= 0) {
            throw new BusinessException("Currency amount to convert must be greater than 0.");
        }

        if (!existingCurrencies.doesCurrencyExist(baseCurrency)) {
            throw new BusinessException("Invalid base currency - does not exist");
        }

        if (!existingCurrencies.anyCurrencyExists(targetCurrencies)) {
            throw new BusinessException(
                    "None of the target currencies exist, please provide correct input values");
        }

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

        // when we can't check cache, fetch from external API
        var currencyRatesResponse =
                currencyAPIClient.fetchCurrencyExchangeRates(baseCurrency, targetCurrencies);
        // save fetched rates to cache
        saveRatesResponseToCache(currencyRatesResponse, targetCurrencies);

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
        // if we do not pass a targetCurrency, only check for A -> (ALL) cached rates
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
            if (reversedConversionRate == null) {
                throw new BusinessException("Source currency does not exist.");
            }
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
            if (conversionRate == null) {
                throw new BusinessException("Target currency does not exist.");
            }
            // A -> B rate is equal to 1/(B -> A) rate
            return Optional.of(
                    new CachedRates(
                            Map.of(targetCurrency.get(), conversionRate),
                            baseToAnyCachedRates.getTimestamp()));
        }

        return Optional.empty();
    }

    /**
     * Saves response obtained to cache, based on what the list of requested currencies were.
     * If no target currencies were supplied, that means we should cache the whole request as a whole,
     * otherwise, we will cache every requested currency individually.
     */
    private void saveRatesResponseToCache(
            CurrencyRatesResponse res, List<String> targetCurrencies) {
        if (targetCurrencies.isEmpty()) {
            cacheService.set(
                    buildCacheKey(res.getSource(), targetCurrencies.stream().findFirst()),
                    new CachedRates(res.getQuotes(), res.getTimestamp()),
                    Duration.ofSeconds(ratesTtlSeconds));
            return;
        }

        for (var currencyExchangePair : res.getQuotes().entrySet()) {
            var target = currencyExchangePair.getKey();
            cacheService.set(
                    buildCacheKey(res.getSource(), Optional.of(target)),
                    new CachedRates(
                            Map.of(target, currencyExchangePair.getValue()), res.getTimestamp()),
                    Duration.ofSeconds(ratesTtlSeconds));
        }
    }

    /**
     * Builds a cache key for caching responses based on baseCurrency and targetCurrency
     * @return If target currency is present, returns 'rates:baseCurrency:targetCurrency'.
     * Otherwise, 'rates:baseCurrency'
     */
    private String buildCacheKey(String baseCurrency, Optional<String> targetCurrency) {
        return cacheKeyBuilderService.buildCacheKey(baseCurrency, targetCurrency);
    }

    private CurrencyConversionDTO mapToConversionDTO(CurrencyRatesResponse res) {
        return new CurrencyConversionDTO(res.getSource(), res.getTimestamp(), res.getQuotes());
    }
}
