package com.exchangerates.CurrencyExchangeAPI.filter;

import com.exchangerates.CurrencyExchangeAPI.security.ApiKeyAuthenticationToken;
import com.exchangerates.CurrencyExchangeAPI.services.interfaces.IAuthenticationService;
import com.exchangerates.CurrencyExchangeAPI.services.interfaces.IRateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@AllArgsConstructor
public class RateLimiterFilter extends OncePerRequestFilter {

    private final IAuthenticationService authenticationService;
    private final IRateLimitService rateLimitService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // extract api key from request
        var apiKey = request.getHeader("X-API-KEY");
        if (apiKey == null) {
            logger.info("Blocked a request due to API key not being provided.");
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.getWriter().write("API key not provided");
            return;
        }

        // check if API key does exist
        if (!authenticationService.isValidAPIKey(apiKey)) {
            logger.info("Blocked a request due to invalid API key");
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.getWriter().write("Invalid API key");
            return;
        }

        // apply rate limiting policy
        if (!rateLimitService.applyRateLimiting(apiKey)) {
            logger.info("Blocked a request due to rate limit exceeded.");
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter()
                    .write("Rate limit exceeded, please wait before making more requests.");
            return;
        }

        // set authentication for this request
        var apiKeyBasedAuth = new ApiKeyAuthenticationToken(apiKey);
        SecurityContextHolder.getContext().setAuthentication(apiKeyBasedAuth);

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        var requestPath = request.getRequestURI();
        return requestPath.contains("/auth");
    }
}
