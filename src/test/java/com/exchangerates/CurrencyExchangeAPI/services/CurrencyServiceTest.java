package com.exchangerates.CurrencyExchangeAPI.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.exchangerates.CurrencyExchangeAPI.contracts.responses.CurrencyConversionDTO;
import com.exchangerates.CurrencyExchangeAPI.contracts.responses.ValueConversionDTO;
import com.exchangerates.CurrencyExchangeAPI.domain.CachedRates;
import com.exchangerates.CurrencyExchangeAPI.domain.CurrencyRatesResponse;
import com.exchangerates.CurrencyExchangeAPI.domain.ExternalAPIError;
import com.exchangerates.CurrencyExchangeAPI.exception.BusinessException;
import com.exchangerates.CurrencyExchangeAPI.services.interfaces.ICacheKeyBuilderService;
import com.exchangerates.CurrencyExchangeAPI.services.interfaces.ICacheService;
import com.exchangerates.CurrencyExchangeAPI.services.interfaces.ICurrencyAPIClient;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CurrencyServiceTest {
    @InjectMocks CurrencyService currencyService;

    @Mock ICurrencyAPIClient currencyAPIClient;

    @Mock ICacheService<CachedRates> cacheService;

    @Mock ICacheKeyBuilderService cacheKeyBuilderService;

    @Test
    void givenValidSourceAndTargetCurrencies_FetchExchangeRateShouldReturnValidResponse() {
        // Arrange
        setupEmptyCacheExpectations();
        String sourceCurrency = "USD";
        String targetCurrency = "EUR";
        double expectedRate = USD_TO_EUR_RATE;
        when(currencyAPIClient.fetchCurrencyExchangeRates(sourceCurrency, List.of(targetCurrency)))
                .thenReturn(usdToEurResponse);

        // Act
        CurrencyConversionDTO response =
                currencyService.getCurrencyConversionRates(
                        sourceCurrency, Optional.of(targetCurrency));

        // Assert
        assertEquals(expectedRate, response.getTargets().get(targetCurrency));
        assertEquals(1, response.getTargets().size()); // only a single target -> EUR
        assertEquals(sourceCurrency, response.getBase());
        // 4 cache misses should happen (A -> B, B -> A, A -> (ALL), B -> (ALL))
        verify(cacheService, times(4)).get(anyString());
        // should save USD -> EUR conversion in cache with some defined TTL
        verify(cacheService, times(1))
                .set(
                        eq(mockBuildCacheKey(sourceCurrency, Optional.of(targetCurrency))),
                        any(CachedRates.class),
                        any(Duration.class));
    }

    @Test
    void
            givenValidSourceCurrencyAndEmptyTarget_FetchExchangeRateShouldReturnConversionForAllAvailableCurrencies() {
        // Arrange
        setupEmptyCacheExpectations();
        String sourceCurrency = "USD";
        double expectedRate = USD_TO_EUR_RATE;
        when(currencyAPIClient.fetchCurrencyExchangeRates(sourceCurrency, Collections.emptyList()))
                .thenReturn(usdToAllResponse);

        // Act
        CurrencyConversionDTO response =
                currencyService.getCurrencyConversionRates(sourceCurrency, Optional.empty());

        // Assert
        assertEquals(expectedRate, response.getTargets().get("EUR"));
        assertEquals(3, response.getTargets().size()); // 3 targets: JPY, CHF, EUR
        assertEquals(sourceCurrency, response.getBase());
        // 1 cache miss - USD -> (ALL)
        verify(cacheService, times(1)).get(anyString());
        // should save USD -> (ALL) conversion in cache with some defined TTL
        verify(cacheService, times(1))
                .set(
                        eq(mockBuildCacheKey(sourceCurrency, Optional.empty())),
                        any(CachedRates.class),
                        any(Duration.class));
    }

    @Test
    void givenBaseAndTargetCurrenciesAreTheSame_FetchExchangeRateShouldThrow() {
        // Arrange
        String sourceCurrency = "USD";
        String targetCurrency = "USD";

        // Act & Assert
        assertThrows(
                BusinessException.class,
                () -> {
                    currencyService.getCurrencyConversionRates(
                            sourceCurrency, Optional.of(targetCurrency));
                });
    }

    @Test
    void givenValidConversionInput_ConvertCurrencyShouldReturnValidResponse() {
        // Arrange
        setupEmptyCacheExpectations();
        double amount = 100.0;
        String sourceCurrency = "USD";
        String targetCurrency = "EUR";
        double expectedRate = USD_TO_EUR_RATE;
        when(currencyAPIClient.fetchCurrencyExchangeRates(sourceCurrency, List.of(targetCurrency)))
                .thenReturn(usdToEurResponse);

        // Act
        ValueConversionDTO conversionResult =
                currencyService.convertCurrencyValues(
                        sourceCurrency, List.of(targetCurrency), amount);

        // Assert
        assertEquals(amount * expectedRate, conversionResult.getConversions().get(targetCurrency));
        assertEquals(sourceCurrency, conversionResult.getBase());
        assertEquals(amount, conversionResult.getRequestedValue());
        // should save USD -> EUR conversion in cache with some defined TTL
        verify(cacheService, times(1))
                .set(
                        eq(mockBuildCacheKey(sourceCurrency, Optional.of(targetCurrency))),
                        any(CachedRates.class),
                        any(Duration.class));
    }

    @Test
    void
            givenConvertToMultipleTargetCurrencies_ConvertCurrencyShouldReturnAndCacheAllTargetCurrencies() {
        // Arrange
        setupCacheKeyBuilderMock();
        double amount = 100.0;
        String sourceCurrency = "USD";
        List<String> targetCurrencies = List.of("EUR", "JPY", "CHF");
        when(currencyAPIClient.fetchCurrencyExchangeRates(sourceCurrency, targetCurrencies))
                .thenReturn(usdToAllResponse);

        // Act
        ValueConversionDTO conversionResult =
                currencyService.convertCurrencyValues(sourceCurrency, targetCurrencies, amount);

        // Assert
        assertEquals(amount * USD_TO_EUR_RATE, conversionResult.getConversions().get("EUR"));
        assertEquals(amount * 100.0, conversionResult.getConversions().get("JPY"));
        assertEquals(amount * 5.0, conversionResult.getConversions().get("CHF"));
        assertEquals(sourceCurrency, conversionResult.getBase());
        assertEquals(amount, conversionResult.getRequestedValue());

        // should save USD -> EUR, USD -> CHF, USD -> JPY conversions in cache with some defined TTL
        for (var targetCurrency : targetCurrencies) {
            verify(cacheService, times(1))
                    .set(
                            eq(mockBuildCacheKey(sourceCurrency, Optional.of(targetCurrency))),
                            any(CachedRates.class),
                            any(Duration.class));
        }
    }

    @Test
    void givenNegativeAmountToConvert_ConvertCurrencyShouldThrow() {
        // Arrange
        double amount = -100.0;
        String sourceCurrency = "USD";
        String targetCurrency = "EUR";

        // Act & Assert
        assertThrows(
                BusinessException.class,
                () -> {
                    currencyService.convertCurrencyValues(
                            sourceCurrency, List.of(targetCurrency), amount);
                });
    }

    @Test
    void givenCachedSourceToTargetConversion_FetchExchangeRateShouldReturnValidResponseFromCache() {
        // Arrange
        setupEmptyCacheExpectations();
        String sourceCurrency = "USD";
        String targetCurrency = "EUR";
        double expectedRate = USD_TO_EUR_RATE;
        // set an expectation on the cache - A -> B rates should be cached
        when(cacheService.get(mockBuildCacheKey(sourceCurrency, Optional.of(targetCurrency))))
                .thenReturn(Optional.of(usdToEurCachedRates));

        // Act
        CurrencyConversionDTO response =
                currencyService.getCurrencyConversionRates(
                        sourceCurrency, Optional.of(targetCurrency));

        // Assert
        assertEquals(expectedRate, response.getTargets().get(targetCurrency));
        assertEquals(1, response.getTargets().size()); // only a single target -> EUR
        assertEquals(sourceCurrency, response.getBase());
        // 1 cache hit should happen (A -> B)
        verify(cacheService, times(1))
                .get(mockBuildCacheKey(sourceCurrency, Optional.of(targetCurrency)));
    }

    @Test
    void givenCachedTargetToSourceConversion_FetchExchangeRateShouldReturnValidResponseFromCache() {
        // Arrange
        setupEmptyCacheExpectations();
        String sourceCurrency = "USD";
        String targetCurrency = "EUR";
        double expectedRate = USD_TO_EUR_RATE;
        // set an expectation on the cache - B -> A rates should be cached
        when(cacheService.get(mockBuildCacheKey(targetCurrency, Optional.of(sourceCurrency))))
                .thenReturn(Optional.of(eurToUSDCachedRates));

        // Act
        CurrencyConversionDTO response =
                currencyService.getCurrencyConversionRates(
                        targetCurrency, Optional.of(sourceCurrency));

        // Assert
        assertEquals(1 / expectedRate, response.getTargets().get(sourceCurrency));
        assertEquals(1, response.getTargets().size()); // only a single target -> USD
        assertEquals(targetCurrency, response.getBase());
        // 1 cache hit should happen (B -> A)
        verify(cacheService, times(1))
                .get(mockBuildCacheKey(targetCurrency, Optional.of(sourceCurrency)));
    }

    @Test
    void givenCachedSourceToAnyConversion_FetchExchangeRateShouldReturnValidResponseFromCache() {
        // Arrange
        setupEmptyCacheExpectations();
        String sourceCurrency = "USD";
        String targetCurrency = "EUR";
        double expectedRate = USD_TO_EUR_RATE;
        // set an expectation on the cache - A -> (ALL) rates should be cached
        when(cacheService.get(mockBuildCacheKey(sourceCurrency, Optional.empty())))
                .thenReturn(Optional.of(usdToAllCachedRates));

        // Act
        CurrencyConversionDTO response =
                currencyService.getCurrencyConversionRates(
                        sourceCurrency, Optional.of(targetCurrency));

        // Assert
        assertEquals(expectedRate, response.getTargets().get(targetCurrency));
        assertEquals(1, response.getTargets().size()); // only a single target -> EUR
        assertEquals(sourceCurrency, response.getBase());
        // 1 cache hit should happen (A -> (ALL)), and we can get A -> B from that
        verify(cacheService, times(1))
                .get(mockBuildCacheKey(sourceCurrency, Optional.of(targetCurrency)));
    }

    @Test
    void givenCachedSourceToAnyWithInvalidTarget_FetchExchangeRateShouldThrow() {
        // Arrange
        setupEmptyCacheExpectations();
        String sourceCurrency = "USD";
        String targetCurrency = "INVALID";
        // set an expectation on the cache - A -> (ALL) rates should be cached
        when(cacheService.get(mockBuildCacheKey(sourceCurrency, Optional.empty())))
                .thenReturn(Optional.of(usdToAllCachedRates));

        // Act & Assert
        assertThrows(
                BusinessException.class,
                () -> {
                    currencyService.getCurrencyConversionRates(
                            sourceCurrency, Optional.of(targetCurrency));
                });
    }

    @Test
    void givenCachedTargetToAnyWithInvalidSource_FetchExchangeRateShouldThrow() {
        // Arrange
        setupEmptyCacheExpectations();
        String sourceCurrency = "INVALID";
        String targetCurrency = "EUR";
        // set an expectation on the cache - B -> (ALL) rates should be cached
        when(cacheService.get(mockBuildCacheKey(targetCurrency, Optional.empty())))
                .thenReturn(Optional.of(eurToAllCachedRates));

        // Act & Assert
        assertThrows(
                BusinessException.class,
                () -> {
                    currencyService.getCurrencyConversionRates(
                            sourceCurrency, Optional.of(targetCurrency));
                });
    }

    @Test
    void givenCachedTargetToAnyConversion_FetchExchangeRateShouldReturnValidResponseFromCache() {
        // Arrange
        setupEmptyCacheExpectations();
        String sourceCurrency = "USD";
        String targetCurrency = "EUR";
        double expectedRate = USD_TO_EUR_RATE;
        // set an expectation on the cache - B -> (ALL) rates should be cached
        when(cacheService.get(mockBuildCacheKey(targetCurrency, Optional.empty())))
                .thenReturn(Optional.of(eurToAllCachedRates));

        // Act
        CurrencyConversionDTO response =
                currencyService.getCurrencyConversionRates(targetCurrency, Optional.empty());

        // Assert
        assertEquals(1 / expectedRate, response.getTargets().get(sourceCurrency));
        assertEquals(
                eurToAllCachedRates.getRates().size(),
                response.getTargets().size()); // only a single target -> USD
        assertEquals(targetCurrency, response.getBase());
        // 1 cache hit should happen (B -> (ALL)), and we can get B -> A, and A -> B from that
        verify(cacheService, times(1)).get(mockBuildCacheKey(targetCurrency, Optional.empty()));
    }

    @Test
    void givenCachedSourceToTargetConversion_ConvertCurrencyShouldReturnValidResponseFromCache() {
        // Arrange
        setupEmptyCacheExpectations();
        double amount = 100.0;
        String sourceCurrency = "USD";
        String targetCurrency = "EUR";
        double expectedRate = USD_TO_EUR_RATE;
        // set an expectation on the cache - A -> B rates should be cached
        when(cacheService.get(mockBuildCacheKey(sourceCurrency, Optional.of(targetCurrency))))
                .thenReturn(Optional.of(usdToEurCachedRates));

        // Act
        ValueConversionDTO conversionResult =
                currencyService.convertCurrencyValues(
                        sourceCurrency, List.of(targetCurrency), amount);

        // Assert
        assertEquals(amount * expectedRate, conversionResult.getConversions().get(targetCurrency));
        assertEquals(sourceCurrency, conversionResult.getBase());
        assertEquals(amount, conversionResult.getRequestedValue());
        // 1 cache hit should happen (A -> B)
        verify(cacheService, times(1))
                .get(mockBuildCacheKey(sourceCurrency, Optional.of(targetCurrency)));
    }

    // also sets up CacheKeyBuilder mock
    private void setupEmptyCacheExpectations() {
        setupCacheKeyBuilderMock();
        when(cacheService.get(anyString())).thenReturn(Optional.empty());
    }

    private void setupCacheKeyBuilderMock() {
        when(cacheKeyBuilderService.buildCacheKey(anyString(), any(Optional.class)))
                .thenAnswer(
                        invocation -> {
                            String sourceCurrency = invocation.getArgument(0);
                            Optional<String> targetCurrency = invocation.getArgument(1);
                            return mockBuildCacheKey(sourceCurrency, targetCurrency);
                        });
    }

    // Mock implementation of buildCacheKey for testing purposes
    private String mockBuildCacheKey(String sourceCurrency, Optional<String> targetCurrency) {
        if (targetCurrency.isEmpty()) {
            return sourceCurrency;
        }

        return sourceCurrency + targetCurrency.get();
    }

    // Static cached rates to use in tests
    private static Instant now = Instant.now();
    private static final double USD_TO_EUR_RATE = 2.0;
    private static final double EUR_TO_USD_RATE = 1 / USD_TO_EUR_RATE;
    private static CurrencyRatesResponse invalidResponse =
            new CurrencyRatesResponse(
                    false,
                    Instant.now(),
                    "",
                    null,
                    new ExternalAPIError(999, "Invalid source currency."));
    private static CurrencyRatesResponse usdToEurResponse =
            new CurrencyRatesResponse(true, now, "USD", Map.of("EUR", USD_TO_EUR_RATE), null);
    private static CurrencyRatesResponse usdToAllResponse =
            new CurrencyRatesResponse(
                    true,
                    now,
                    "USD",
                    Map.of("EUR", USD_TO_EUR_RATE, "JPY", 100.0, "CHF", 5.0),
                    null);
    private static CachedRates usdToEurCachedRates =
            new CachedRates(Map.of("EUR", USD_TO_EUR_RATE), now);
    private static CachedRates eurToUSDCachedRates =
            new CachedRates(Map.of("USD", EUR_TO_USD_RATE), now);
    private static CachedRates usdToAllCachedRates =
            new CachedRates(Map.of("EUR", USD_TO_EUR_RATE, "JPY", 100.0, "CHF", 5.0), now);
    private static CachedRates eurToAllCachedRates =
            new CachedRates(Map.of("USD", EUR_TO_USD_RATE, "JPY", 100.0, "CHF", 5.0), now);
}
