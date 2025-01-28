package com.exchangerates.CurrencyExchangeAPI.configuration;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.ResourceScriptSource;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        var redisTemplate = new RedisTemplate<String, Object>();
        redisTemplate.setConnectionFactory(connectionFactory);

        // for serializing java.time.Instant
        var mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
        mapper.activateDefaultTyping(
                mapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);

        var serializer = new GenericJackson2JsonRedisSerializer(mapper);

        redisTemplate.setDefaultSerializer(serializer);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        return redisTemplate;
    }

    // Documentation for Redis script
    // https://docs.spring.io/spring-data/redis/reference/redis/scripting.html
    // using DefaultRedisScript instead of RedisScript to avoid recalculating SHA1 hash multiple
    // times,
    // which makes the script get cached in Redis, which is faster
    @Bean
    public DefaultRedisScript<Boolean> rateLimitingScript() {
        ScriptSource scriptSource =
                new ResourceScriptSource(new ClassPathResource("scripts/ratelimiter.lua"));
        DefaultRedisScript<Boolean> script = new DefaultRedisScript<>();
        script.setScriptSource(scriptSource);
        script.setResultType(Boolean.class);
        return script;
    }
}
