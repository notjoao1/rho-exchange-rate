package com.exchangerates.CurrencyExchangeAPI.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

import com.exchangerates.CurrencyExchangeAPI.filter.RateLimiterFilter;
import com.exchangerates.CurrencyExchangeAPI.services.interfaces.IAuthenticationService;
import com.exchangerates.CurrencyExchangeAPI.services.interfaces.IRateLimitService;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final IAuthenticationService authenticationService;
    private final IRateLimitService rateLimitService;

    @Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.addFilterBefore(new RateLimiterFilter(authenticationService, rateLimitService), AnonymousAuthenticationFilter.class);

        // allow requests to /auth, and docs related endpoints, and prevent all others
		http
            .csrf(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests((requests) -> requests
                    .requestMatchers(
                        "/api/v1/auth/**",
                        "/api/docs/**",
                        "/api/swagger-ui/**")
                    .permitAll()
				    .anyRequest()
                    .authenticated()
			);

		return http.build();
	}

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
