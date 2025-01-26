package com.exchangerates.CurrencyExchangeAPI.services.interfaces;

public interface IRateLimitService {
    /**
     * Applies rate limits, and returns whether the request is allowed to proceed.
     * @param apiKey The API key to check the rate limit for
     * @return True if the rate limit has been exceeded, false otherwise
     */
    public boolean applyRateLimiting(String rateLimitKey);
}
