package com.exchangerates.CurrencyExchangeAPI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.exchangerates.CurrencyExchangeAPI.services.RedisCacheService;

@ExtendWith(MockitoExtension.class)
class CacheServiceTest {
    @Mock 
    RedisTemplate<String, Integer> redisTemplate;

    @InjectMocks 
    RedisCacheService<Integer> cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new RedisCacheService<>(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(mock(ValueOperations.class));
    }
    
    @Test
    void givenExistingKeys_GetShouldReturnAssociatedValues() {
        // Arrange
        var key = "test:key";
        Integer value = 505050;
        when(redisTemplate.opsForValue().get(key)).thenReturn(value);

        // Act
        Integer result = cacheService.get(key).get();

        // Assert
        assertEquals(value, result);
    }

    @Test
    void givenNonExistentKey_GetShouldReturnEmptyOptional() {
        // Arrange
        var key = "test:key";
        when(redisTemplate.opsForValue().get(key)).thenReturn(null);

        // Act 
        var optionalResult = cacheService.get(key);

        // Assert
        assertTrue(optionalResult.isEmpty());
    }

    @Test
    void givenKeyAndValue_SetShouldStoreValueInCache() {
        // Arrange
        var key = "test:key";
        Integer value = 505050;
    
        // Act
        cacheService.set(key, value);

        // Assert
        verify(redisTemplate.opsForValue(), times(1)).set(key, value);
    }

    @Test
    void givenKeyAndValueAndTTL_SetShouldStoreValueInCacheWithTTL() {
        // Arrange
        var key = "test:key";
        Integer value = 505050;
        var ttl = java.time.Duration.ofSeconds(10);

        // Act
        cacheService.set(key, value, ttl);

        // Assert
        verify(redisTemplate.opsForValue(), times(1)).set(key, value, ttl);
    }
}
