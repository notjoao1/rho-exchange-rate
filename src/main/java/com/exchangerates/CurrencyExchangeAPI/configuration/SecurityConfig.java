package com.exchangerates.CurrencyExchangeAPI.configuration;

import com.exchangerates.CurrencyExchangeAPI.filter.RateLimiterFilter;
import com.exchangerates.CurrencyExchangeAPI.services.interfaces.IAuthenticationService;
import com.exchangerates.CurrencyExchangeAPI.services.interfaces.IRateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final IAuthenticationService authenticationService;
    private final IRateLimitService rateLimitService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // allow requests to /auth, and docs related endpoints, and prevent all others
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(
                        (requests) ->
                                requests.requestMatchers(
                                                "/error",
                                                "/api/v1/auth/**",
                                                "/api/docs/**",
                                                "/api/swagger-ui/**")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .addFilterBefore(
                        new RateLimiterFilter(authenticationService, rateLimitService),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
