package com.exchangerates.CurrencyExchangeAPI.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI currencyExchangeAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Currency Exchange")
                                .description(
                                        "API for currency conversion and monitoring exchange rates"
                                                + " between different currencies.")
                                .version("1.0.0"))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        "api_key",
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.APIKEY)
                                                .in(SecurityScheme.In.HEADER)
                                                .name("X-API-KEY")));
    }
}
