package com.exchangerates.CurrencyExchangeAPI.services;

import com.exchangerates.CurrencyExchangeAPI.services.interfaces.ICacheKeyBuilderService;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class CacheKeyBuilderService implements ICacheKeyBuilderService {

    @Override
    public String buildCacheKey(String baseCurrency, Optional<String> targetCurrency) {
        return targetCurrency.isPresent()
                ? String.format("rates:%s:%s", baseCurrency, targetCurrency.get())
                : String.format("rates:%s", baseCurrency);
    }
}
