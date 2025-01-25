package com.exchangerates.CurrencyExchangeAPI.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

import com.exchangerates.CurrencyExchangeAPI.filter.RateLimiterFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.addFilterBefore(new RateLimiterFilter(), AnonymousAuthenticationFilter.class);

        // allow requests to /auth, and docs related endpoints, and prevent all others
		http
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
        // OWASP recommendation
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}
