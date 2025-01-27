package com.exchangerates.CurrencyExchangeAPI.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.exchangerates.CurrencyExchangeAPI.domain.AvailableCurrenciesResponse;
import com.exchangerates.CurrencyExchangeAPI.services.interfaces.ICacheService;
import com.exchangerates.CurrencyExchangeAPI.services.interfaces.ICurrencyAPIClient;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AvailableCurrenciesHolderTest {

    @Mock ICacheService<Set<String>> cacheService;

    @Mock ICurrencyAPIClient currencyAPIClient;

    @InjectMocks AvailableCurrenciesHolder holder;

    @Test
    void getAvailableCurrencies_WhenCached_ReturnsCachedData() {
        // Arrange
        Set<String> currencies = new HashSet<>(Set.of("USD", "EUR"));
        when(cacheService.get(anyString())).thenReturn(Optional.of(currencies));

        // Act
        Set<String> result = holder.getAvailableCurrencies();

        // Assert
        assertEquals(currencies, result);
        verify(currencyAPIClient, times(0)).fetchAvailableCurrencies();

        // reset mock and check that getAvailableCurrencies returns the same thing with
        // no cache checking or external API calls
        reset(cacheService, currencyAPIClient);

        // act (again)
        result = holder.getAvailableCurrencies();
        verify(currencyAPIClient, times(0)).fetchCurrencyExchangeRates(anyString(), anyList());
        verify(cacheService, times(0)).get(anyString());
    }

    @Test
    void getAvailableCurrencies_WhenNotCached_FetchesFromAPI() {
        // Arrange
        Set<String> currencies = new HashSet<>(Set.of("USD", "EUR"));
        when(cacheService.get(anyString())).thenReturn(Optional.empty());

        AvailableCurrenciesResponse apiResponse = new AvailableCurrenciesResponse();
        apiResponse.setSuccess(true);
        apiResponse.setCurrencies(Map.of("USD", "US Dollar", "EUR", "Euro"));
        when(currencyAPIClient.fetchAvailableCurrencies()).thenReturn(apiResponse);

        // Act
        Set<String> result = holder.getAvailableCurrencies();

        // Assert
        assertEquals(currencies, result);
        verify(currencyAPIClient, times(1)).fetchAvailableCurrencies();
        verify(cacheService, times(1)).get(anyString());
        verify(cacheService, times(1)).set(anyString(), any(), any(Duration.class));
    }

    @Test
    void doesCurrencyExist_WithNull_ReturnsFalse() {
        assertFalse(holder.doesCurrencyExist(null));
    }

    @Test
    void doesCurrencyExist_WithExistingCurrency_ReturnsTrue() {
        // Arrange
        Set<String> currencies = new HashSet<>(Set.of("USD", "EUR"));
        when(cacheService.get(anyString())).thenReturn(Optional.of(currencies));

        // Act & Assert
        assertTrue(holder.doesCurrencyExist("USD"));
    }

    @Test
    void doesCurrencyExist_WithNonExistingCurrency_ReturnsFalse() {
        // Arrange
        Set<String> currencies = new HashSet<>(Set.of("USD", "EUR"));
        when(cacheService.get(anyString())).thenReturn(Optional.of(currencies));

        // Act & Assert
        assertFalse(holder.doesCurrencyExist("GBP"));
    }

    @Test
    void anyCurrencyExists_WithEmptyList_ReturnsFalse() {
        assertFalse(holder.anyCurrencyExists(List.of()));
    }

    @Test
    void anyCurrencyExists_WithNullList_ReturnsFalse() {
        assertFalse(holder.anyCurrencyExists(null));
    }

    @Test
    void anyCurrencyExists_WithMatchingCurrency_ReturnsTrue() {
        // Arrange
        Set<String> currencies = new HashSet<>(Set.of("USD", "EUR"));
        when(cacheService.get(anyString())).thenReturn(Optional.of(currencies));

        // Act & Assert
        assertTrue(holder.anyCurrencyExists(List.of("USD", "GBP")));
    }
}
