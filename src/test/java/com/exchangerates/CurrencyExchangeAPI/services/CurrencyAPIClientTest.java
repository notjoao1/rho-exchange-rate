package com.exchangerates.CurrencyExchangeAPI.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.exchangerates.CurrencyExchangeAPI.domain.AvailableCurrenciesResponse;
import com.exchangerates.CurrencyExchangeAPI.domain.CurrencyRatesResponse;
import com.exchangerates.CurrencyExchangeAPI.domain.ExternalAPIError;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CurrencyAPIClientTest {

    @Mock RestTemplate restTemplate;

    @InjectMocks CurrencyAPIClient currencyAPIClient;

    @Test
    void fetchAvailableCurrencies_Success() {
        // Arrange
        var response = new AvailableCurrenciesResponse();
        response.setSuccess(true);
        var currencies = new HashMap<String, String>();
        currencies.put("USD", "US Dollar");
        currencies.put("EUR", "Euro");
        response.setCurrencies(currencies);

        when(restTemplate.getForObject(anyString(), eq(AvailableCurrenciesResponse.class)))
                .thenReturn(response);

        // Act
        var result = currencyAPIClient.fetchAvailableCurrencies();

        // Assert
        verify(restTemplate, times(1))
                .getForObject(anyString(), eq(AvailableCurrenciesResponse.class));
        assertTrue(result.isSuccess());
        assertEquals(2, result.getCurrencies().size());
    }

    @Test
    void fetchAvailableCurrencies_WhenApiFails_ThrowsException() {
        // Arrange
        var response = new AvailableCurrenciesResponse();
        response.setSuccess(false);
        response.setError(new ExternalAPIError(999, "Failed to fetch available currencies"));

        when(restTemplate.getForObject(anyString(), eq(AvailableCurrenciesResponse.class)))
                .thenReturn(response);

        // Act & Assert
        assertThrows(
                ResponseStatusException.class, () -> currencyAPIClient.fetchAvailableCurrencies());
    }

    @Test
    void fetchCurrencyExchangeRatesSingleTarget_Success() {
        // Arrange
        var sourceCurrency = "USD";
        var targetCurrency = "EUR";
        var rate = 2.0;
        var response =
                new CurrencyRatesResponse(
                        true, Instant.now(), sourceCurrency, Map.of("USDEUR", rate), null);

        when(restTemplate.getForObject(anyString(), eq(CurrencyRatesResponse.class)))
                .thenReturn(response);

        // Act
        var result = currencyAPIClient.fetchCurrencyExchangeRates("USD", List.of("EUR"));

        // Assert
        verify(restTemplate, times(1)).getForObject(anyString(), eq(CurrencyRatesResponse.class));
        assertEquals(sourceCurrency, result.getSource());
        assertNotNull(result.getQuotes());
        assertEquals(1, result.getQuotes().size());
        assertNotNull(result.getQuotes().get(targetCurrency));
        assertEquals(rate, result.getQuotes().get(targetCurrency));
    }

    @Test
    void fetchCurrencyExchangeRatesMultiTarget_Success() {
        // Arrange
        var sourceCurrency = "USD";
        var targetCurrency1 = "EUR";
        var targetCurrency2 = "JPY";
        var rate1 = 2.0;
        var rate2 = 100.0;
        var response =
                new CurrencyRatesResponse(
                        true,
                        Instant.now(),
                        sourceCurrency,
                        Map.of("USDEUR", rate1, "USDJPY", rate2),
                        null);

        when(restTemplate.getForObject(anyString(), eq(CurrencyRatesResponse.class)))
                .thenReturn(response);

        // Act
        var result = currencyAPIClient.fetchCurrencyExchangeRates("USD", List.of("EUR", "JPY"));

        // Assert
        verify(restTemplate, times(1)).getForObject(anyString(), eq(CurrencyRatesResponse.class));
        assertEquals(sourceCurrency, result.getSource());
        assertNotNull(result.getQuotes());
        assertNotNull(result.getQuotes().get(targetCurrency1));
        assertEquals(rate1, result.getQuotes().get(targetCurrency1));
        assertNotNull(result.getQuotes().get(targetCurrency2));
        assertEquals(rate2, result.getQuotes().get(targetCurrency2));
    }

    @Test
    void fetchCurrencyExchangeRates_WhenApiFails_ThrowsException() {
        // Arrange
        var response = new CurrencyRatesResponse();
        response.setSuccess(false);
        response.setError(new ExternalAPIError(999, "Failed to fetch exchange rates"));

        when(restTemplate.getForObject(anyString(), eq(CurrencyRatesResponse.class)))
                .thenReturn(response);

        // Act & Assert
        assertThrows(
                ResponseStatusException.class,
                () -> currencyAPIClient.fetchCurrencyExchangeRates("USD", List.of("EUR")));
    }
}
