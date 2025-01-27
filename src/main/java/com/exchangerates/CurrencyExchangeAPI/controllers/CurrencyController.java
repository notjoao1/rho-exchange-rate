package com.exchangerates.CurrencyExchangeAPI.controllers;

import com.exchangerates.CurrencyExchangeAPI.contracts.responses.CurrencyConversionDTO;
import com.exchangerates.CurrencyExchangeAPI.contracts.responses.ValueConversionDTO;
import com.exchangerates.CurrencyExchangeAPI.services.interfaces.ICurrencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "api_key")
@Tag(
        name = "Currency Exchange",
        description = "Endpoints for currency conversion and exchange rates")
@RestController
@RequestMapping("/api/v1/currency")
@AllArgsConstructor
public class CurrencyController {
    private final ICurrencyService currencyService;

    @Operation(
            summary = "List currency conversion rates.",
            description =
                    "Fetches exchange rates from a base currency to one or all target currencies.")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description =
                                "Lists currency conversion rate between provided base and target"
                                        + " currency. If target currency is not provided, lists"
                                        + " conversion rate between base currency and all other"
                                        + " available currencies",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                CurrencyConversionDTO.class))),
                @ApiResponse(
                        responseCode = "400",
                        description =
                                "Invalid currency code provided, or the base currency is the same"
                                        + " as the target currency",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ErrorResponse.class))),
                @ApiResponse(responseCode = "429", description = "Rate limit exceeded"),
                @ApiResponse(
                        responseCode = "502",
                        description = "Upstream server returned invalid response")
            })
    @GetMapping("rate")
    public ResponseEntity<CurrencyConversionDTO> getConversionRateBetweenCurrencies(
            @Valid @NotEmpty @RequestParam("from") String baseCurrency,
            @RequestParam("to") Optional<String> targetCurrency) {
        return ResponseEntity.ok(
                currencyService.getCurrencyConversionRates(baseCurrency, targetCurrency));
    }

    @Operation(
            summary = "Convert currency values",
            description =
                    "Converts a given amount from base currency to one or more target currencies")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successfully converted the amount to target currencies",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ValueConversionDTO.class))),
                @ApiResponse(
                        responseCode = "400",
                        description =
                                "Invalid currency code or provided value to convert is 0 or"
                                        + " negative.",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ErrorResponse.class))),
                @ApiResponse(responseCode = "429", description = "Rate limit exceeded"),
                @ApiResponse(
                        responseCode = "502",
                        description = "Upstream server returned invalid response")
            })
    @GetMapping("convert")
    public ResponseEntity<ValueConversionDTO> getConversionRateBetweenCurrencies(
            @Valid @NotEmpty @RequestParam("from") String baseCurrency,
            @Valid @NotEmpty @RequestParam("to") String targetCurrencies,
            @Valid @Positive @NotNull @RequestParam("value") double valueToConvert) {
        // parse 'to' query parameter: a comma separated list of currencies encoded in
        // a single string. Example: 'USD,AUD,CAD,EUR,CHF' -> 'USD,AUD,CAD,EUR,CHF'
        var currenciesToConvertTo = targetCurrencies.toUpperCase().split(",");
        return ResponseEntity.ok(
                currencyService.convertCurrencyValues(
                        baseCurrency, Arrays.asList(currenciesToConvertTo), valueToConvert));
    }
}
