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
import com.exchangerates.CurrencyExchangeAPI.domain.CurrencyRatesResponseError;
import com.exchangerates.CurrencyExchangeAPI.exception.BusinessException;
import com.exchangerates.CurrencyExchangeAPI.services.interfaces.ICacheKeyBuilderService;
import com.exchangerates.CurrencyExchangeAPI.services.interfaces.ICacheService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CurrencyServiceTest {
    @InjectMocks CurrencyService currencyService;

    @Mock RestTemplate restTemplate;

    @Mock ICacheService<CachedRates> cacheService;

    @Mock ICacheKeyBuilderService cacheKeyBuilderService;

    @BeforeEach
    void setup() {
        // setup our own mock buildCacheKey
    }

    @Test
    void givenValidSourceAndTargetCurrencies_FetchExchangeRateShouldReturnValidResponse() {
        // Arrange
        setupEmptyCacheExpectations();
        String sourceCurrency = "USD";
        String targetCurrency = "EUR";
        double expectedRate = USD_TO_EUR_RATE;
        when(restTemplate.getForObject(anyString(), eq(CurrencyRatesResponse.class)))
                .thenReturn(
                        new CurrencyRatesResponse(
                                true,
                                Instant.now(),
                                sourceCurrency,
                                Map.of(targetCurrency, expectedRate),
                                null));

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
    }

    @Test
    void
            givenValidSourceCurrencyAndEmptyTarget_FetchExchangeRateShouldReturnConversionForAllAvailableCurrencies() {
        // Arrange
        setupEmptyCacheExpectations();
        String sourceCurrency = "USD";
        double expectedRate = USD_TO_EUR_RATE;
        when(restTemplate.getForObject(anyString(), eq(CurrencyRatesResponse.class)))
                .thenReturn(
                        new CurrencyRatesResponse(
                                true,
                                Instant.now(),
                                sourceCurrency,
                                Map.of("EUR", expectedRate),
                                null));

        // Act
        CurrencyConversionDTO response =
                currencyService.getCurrencyConversionRates(sourceCurrency, Optional.empty());

        // Assert
        assertEquals(expectedRate, response.getTargets().get("EUR"));
        assertEquals(1, response.getTargets().size()); // only a single target -> EUR
        assertEquals(sourceCurrency, response.getBase());
        // 4 cache misses should happen (A -> B, B -> A, A -> (ALL), B -> (ALL))
        verify(cacheService, times(1)).get(anyString());
    }

    @Test
    void givenInvalidSourceCurrency_FetchExchangeRateShouldThrow() {
        // Arrange
        setupEmptyCacheExpectations();
        String sourceCurrency = "INVALID";
        String targetCurrency = "EUR";
        when(restTemplate.getForObject(anyString(), eq(CurrencyRatesResponse.class)))
                .thenReturn(
                        new CurrencyRatesResponse(
                                false,
                                Instant.now(),
                                sourceCurrency,
                                null,
                                new CurrencyRatesResponseError(999, "Invalid source currency.")));

        // Act & Assert
        assertThrows(
                ResponseStatusException.class,
                () -> {
                    currencyService.getCurrencyConversionRates(
                            sourceCurrency, Optional.of(targetCurrency));
                });
    }

    @Test
    void givenInvalidTargetCurrency_FetchExchangeRateShouldThrow() {
        // Arrange
        String sourceCurrency = "USD";
        String targetCurrency = "INVALID";
        when(restTemplate.getForObject(anyString(), eq(CurrencyRatesResponse.class)))
                .thenReturn(
                        new CurrencyRatesResponse(
                                false,
                                Instant.now(),
                                sourceCurrency,
                                null,
                                new CurrencyRatesResponseError(999, "Invalid target currency.")));

        // Act & Assert
        assertThrows(
                ResponseStatusException.class,
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
        when(restTemplate.getForObject(anyString(), eq(CurrencyRatesResponse.class)))
                .thenReturn(
                        new CurrencyRatesResponse(
                                true,
                                Instant.now(),
                                sourceCurrency,
                                Map.of(targetCurrency, expectedRate),
                                null));

        // Act
        ValueConversionDTO conversionResult =
                currencyService.convertCurrencyValues(
                        sourceCurrency, List.of(targetCurrency), amount);

        // Assert
        assertEquals(amount * expectedRate, conversionResult.getConversions().get(targetCurrency));
        assertEquals(sourceCurrency, conversionResult.getBase());
        assertEquals(amount, conversionResult.getRequestedValue());
    }

    @Test
    void givenInvalidTargetCurrency_ConvertCurrencyShouldReturnValidResponse() {
        // Arrange
        double amount = 100.0;
        String sourceCurrency = "USD";
        String targetCurrency = "INVALID";
        when(restTemplate.getForObject(anyString(), eq(CurrencyRatesResponse.class)))
                .thenReturn(
                        new CurrencyRatesResponse(
                                false,
                                Instant.now(),
                                sourceCurrency,
                                Map.of("EUR", 1.2),
                                new CurrencyRatesResponseError(999, "Invalid target currency")));

        // Act & Assert
        assertThrows(
                ResponseStatusException.class,
                () -> {
                    currencyService.convertCurrencyValues(
                            sourceCurrency, List.of(targetCurrency), amount);
                });
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
                .thenReturn(Optional.of(usdToEur));

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
                .thenReturn(Optional.of(eurToUSD));

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
                .thenReturn(Optional.of(usdToAll));

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
    void givenCachedTargetToAnyConversion_FetchExchangeRateShouldReturnValidResponseFromCache() {
        // Arrange
        setupEmptyCacheExpectations();
        String sourceCurrency = "USD";
        String targetCurrency = "EUR";
        double expectedRate = USD_TO_EUR_RATE;
        // set an expectation on the cache - B -> (ALL) rates should be cached
        when(cacheService.get(mockBuildCacheKey(targetCurrency, Optional.empty())))
                .thenReturn(Optional.of(eurToAll));

        // Act
        CurrencyConversionDTO response =
                currencyService.getCurrencyConversionRates(targetCurrency, Optional.empty());

        // Assert
        assertEquals(1 / expectedRate, response.getTargets().get(sourceCurrency));
        assertEquals(
                eurToAll.getRates().size(),
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
                .thenReturn(Optional.of(usdToEur));

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

    private void setupEmptyCacheExpectations() {
        when(cacheKeyBuilderService.buildCacheKey(anyString(), any(Optional.class)))
                .thenAnswer(
                        invocation -> {
                            String sourceCurrency = invocation.getArgument(0);
                            Optional<String> targetCurrency = invocation.getArgument(1);
                            return mockBuildCacheKey(sourceCurrency, targetCurrency);
                        });
        when(cacheService.get(anyString())).thenReturn(Optional.empty());
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
    private static CachedRates usdToEur = new CachedRates(Map.of("EUR", USD_TO_EUR_RATE), now);
    private static CachedRates eurToUSD = new CachedRates(Map.of("USD", EUR_TO_USD_RATE), now);
    private static CachedRates usdToAll =
            new CachedRates(Map.of("EUR", USD_TO_EUR_RATE, "JPY", 100.0, "CHF", 5.0), now);
    private static CachedRates eurToAll =
            new CachedRates(Map.of("USD", EUR_TO_USD_RATE, "JPY", 100.0, "CHF", 5.0), now);
}
