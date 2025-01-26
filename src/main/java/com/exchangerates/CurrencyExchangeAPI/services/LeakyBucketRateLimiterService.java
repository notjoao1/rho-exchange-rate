package com.exchangerates.CurrencyExchangeAPI.services;

import com.exchangerates.CurrencyExchangeAPI.services.interfaces.IRateLimitService;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class LeakyBucketRateLimiterService implements IRateLimitService {

    // TODO: this rate limiter is very coupled to Redis right now, which is not ideal
    private final RedisOperations<String, Object> redisOperations;
    // Script inputs: 1 key + 3 arguments - time of the request (unix epoch), the leak rate of
    // buckets, and the maximum capacity
    private final RedisScript<Boolean> leakyBucketScript;

    private static final int DRAIN_RATE = 1;
    private static final int LIMIT = 10;

    @Override
    public boolean applyRateLimiting(String rateLimitKey) {
        // returns boolean representing whether the request will be allowed for this apiKey
        return redisOperations.execute(
                leakyBucketScript,
                List.of(rateLimitKey),
                Instant.now().getEpochSecond(),
                DRAIN_RATE,
                LIMIT);
    }
}
