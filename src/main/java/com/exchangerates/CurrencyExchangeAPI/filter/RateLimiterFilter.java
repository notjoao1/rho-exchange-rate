package com.exchangerates.CurrencyExchangeAPI.filter;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import com.exchangerates.CurrencyExchangeAPI.services.interfaces.IAuthenticationService;
import com.exchangerates.CurrencyExchangeAPI.services.interfaces.IRateLimitService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RateLimiterFilter extends OncePerRequestFilter {

    private final IAuthenticationService authenticationService;
    private final IRateLimitService rateLimitService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // extract api key from request
        var apiKey = request.getHeader("X-API-KEY");
        if (apiKey == null) {
            response.sendError(HttpStatus.FORBIDDEN.value(), "API key not provided");
            return;
        }

        // check if API key does exist
        if (!authenticationService.isValidAPIKey(apiKey)) {
            response.sendError(HttpStatus.FORBIDDEN.value(), "Invalid API key");
            return;
        }

        // check rate limit
        if (rateLimitService.isRateLimitExceeded(apiKey)) {
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "Rate limit exceeded, please wait before making more requests.");
        }

        // register request for rate limiting
        rateLimitService.incrementRequestCount(apiKey);

        filterChain.doFilter(request, response);
    }
    
}
