package com.exchangerates.CurrencyExchangeAPI.controllers;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.exchangerates.CurrencyExchangeAPI.contracts.responses.CurrencyConversionDTO;
import com.exchangerates.CurrencyExchangeAPI.contracts.responses.ValueConversionDTO;
import com.exchangerates.CurrencyExchangeAPI.services.interfaces.ICurrencyService;

import io.restassured.http.ContentType;

@WebMvcTest(CurrencyController.class)
@AutoConfigureMockMvc(addFilters = false) // not testing for security here
class CurrencyControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private ICurrencyService currencyService;
 
    @Test
    void givenValidSourceAndTargetCurrencies_FetchExchangeRates_ShouldSucceed() {
        // Arrange
        var baseCurrency = "USD";
        var targetCurrency = "EUR";

        when(currencyService.getCurrencyConversionRates(baseCurrency, Optional.of(targetCurrency)))
            .thenReturn(usdToEur);

        // Act & Assert
        given().
            mockMvc(mockMvc).
            contentType(ContentType.JSON).
            param("from", baseCurrency).
            param("to", targetCurrency).
        when().
            get("/api/v1/currency/rate").
        then().
            statusCode(HttpStatus.SC_OK).
            and().
            body("base", is(baseCurrency),
                 "targets.EUR", is(notNullValue()),
                  "targets.EUR", is((float) USD_TO_EUR_RATE), // hamcrest needs floats
                  "targets.size()", equalTo(1));
    }

    
    @Test
    void givenEmptySourceCurrency_FetchConversionRates_ShouldReturnBadRequest() {
        // Arrange
        var baseCurrency = ""; // should throw due to failed validation
        var targetCurrency = "EUR";

        // Act & Assert
        given().
            mockMvc(mockMvc).
            contentType(ContentType.JSON).
            param("from", baseCurrency).
            param("to", targetCurrency).
        when().
            get("/api/v1/currency/rate").
        then().
            statusCode(HttpStatus.SC_BAD_REQUEST).
            and().
            body("message", is(notNullValue()));
    }

    @Test
    void givenEmptyTargetCurrency_FetchConversionRates_ShouldSucceedAndFetchRatesForAllCurrencies() {
        // Arrange
        var baseCurrency = "USD";

        when(currencyService.getCurrencyConversionRates(baseCurrency, Optional.empty()))
            .thenReturn(usdToAll);

        // Act & Assert
        given().
            mockMvc(mockMvc).
            contentType(ContentType.JSON).
            param("from", baseCurrency).
        when().
            get("/api/v1/currency/rate").
        then().
            statusCode(HttpStatus.SC_OK).
            and().
            body("base", is(baseCurrency),
                 "targets", hasKey("EUR"),
                 "targets", hasKey("CHF"),
                 "targets", hasKey("JPY"),
                  "targets.EUR", is((float) USD_TO_EUR_RATE),
                  "targets.size()", equalTo(3));
    }

    @Test
    void givenInvalidSourceCurrency_FetchConversionRates_ShouldReturnBadRequest() {
        // Arrange
        var baseCurrency = "INVALID";
        var targetCurrency = "EUR";

        when(currencyService.getCurrencyConversionRates(baseCurrency, Optional.of(targetCurrency)))
            .thenThrow(new ResponseStatusException(HttpStatusCode.valueOf(400), "Currency not found"));

        // Act & Assert
        given().
            mockMvc(mockMvc).
            contentType(ContentType.JSON).
            param("from", baseCurrency).
            param("to", targetCurrency).
        when().
            get("/api/v1/currency/rate").
        then().
            statusCode(HttpStatus.SC_BAD_REQUEST).
            and().
            body("message", is(notNullValue()));
    }

    @Test
    void givenInvalidTargetCurrency_FetchConversionRates_ShouldReturnBadRequest() {
        // Arrange
        var baseCurrency = "USD";
        var targetCurrency = "INVALID";

        when(currencyService.getCurrencyConversionRates(baseCurrency, Optional.of(targetCurrency)))
            .thenThrow(new ResponseStatusException(HttpStatusCode.valueOf(400), "Currency not found"));

        // Act & Assert
        given().
            mockMvc(mockMvc).
            contentType(ContentType.JSON).
            param("from", baseCurrency).
            param("to", targetCurrency).
        when().
            get("/api/v1/currency/rate").
        then().
            statusCode(HttpStatus.SC_BAD_REQUEST).
            and().
            body("message", is(notNullValue()));

    }

    @Test
    void givenValidSourceAndTargetCurrenciesAndAmount_ConvertCurrency_ShouldSucceed() {
        // Arrange
        var baseCurrency = "USD";
        var targetCurrencies = "EUR";
        var amount = 100.0;

        when(currencyService.convertCurrencyValues(baseCurrency, Arrays.asList(targetCurrencies), amount))
            .thenReturn(new ValueConversionDTO(baseCurrency, amount, Map.of("EUR", amount * USD_TO_EUR_RATE)));

        // Act & Assert
        given().
            mockMvc(mockMvc).
            contentType(ContentType.JSON).
            param("from", "USD").
            param("to", "EUR").
            param("value", "100").
        when().
            get("/api/v1/currency/convert").
        then().
            statusCode(HttpStatus.SC_OK).
            and().
            body("base", is(baseCurrency),
                 "conversions.EUR", is(notNullValue()),
                 "conversions.EUR", is((float) (amount * USD_TO_EUR_RATE)),
                 "conversions.size()", equalTo(1));
    }

    @Test
    void givenInvalidSourceCurrency_ConvertCurrency_ShouldReturnBadRequest() {
        // Arrange
        var baseCurrency = "INVALID";
        var targetCurrencies = "EUR";
        var amount = 100.0;

        when(currencyService.convertCurrencyValues(baseCurrency, Arrays.asList(targetCurrencies), amount))
            .thenThrow(new ResponseStatusException(HttpStatusCode.valueOf(400), "Currency not found"));

        // Act & Assert
        given().
            mockMvc(mockMvc).
            contentType(ContentType.JSON).
            param("from", baseCurrency).
            param("to", targetCurrencies).
            param("value", amount).
        when().
            get("/api/v1/currency/convert").
        then().
            statusCode(HttpStatus.SC_BAD_REQUEST).
            and().
            body("message", is(notNullValue()));
    }

    @Test
    void givenInvalidTargetCurrency_ConvertCurrency_ShouldReturnBadRequest() {
        // Arrange
        var baseCurrency = "USD";
        var targetCurrencies = "INVALID";
        var amount = 100.0;

        when(currencyService.convertCurrencyValues(baseCurrency, Arrays.asList(targetCurrencies), amount))
            .thenThrow(new ResponseStatusException(HttpStatusCode.valueOf(400), "Currency not found"));

        // Act & Assert
        given().
            mockMvc(mockMvc).
            contentType(ContentType.JSON).
            param("from", baseCurrency).
            param("to", targetCurrencies).
            param("value", amount).
        when().
            get("/api/v1/currency/convert").
        then().
            statusCode(HttpStatus.SC_BAD_REQUEST).
            and().
            body("message", is(notNullValue()));
    }

    @Test
    void givenInvalidAmountToConvert_ConvertCurrency_ShouldReturnBadRequest() {
        // Arrange
        var baseCurrency = "USD";
        var targetCurrencies = "EUR";
        var amount = -100.0;

        // Act & Assert
        given().
            mockMvc(mockMvc).
            contentType(ContentType.JSON).
            param("from", baseCurrency).
            param("to", targetCurrencies).
            param("value", amount).
        when().
            get("/api/v1/currency/convert").
        then().
            statusCode(HttpStatus.SC_BAD_REQUEST).
            and().
            body("message", is(notNullValue()));
    }
    
    @Test
    void givenValidMultipleTargetCurrencies_ConvertCurrency_ShouldReturnConversionForAllOfThem() {
        // Arrange
        var baseCurrency = "USD";
        var targetCurrencies = "EUR,CHF,JPY";
        var amount = 100.0;

        when(currencyService.convertCurrencyValues(baseCurrency, Arrays.asList(targetCurrencies.split(",")), amount))
            .thenReturn(new ValueConversionDTO(baseCurrency, amount, 
                Map.of("EUR", amount * USD_TO_EUR_RATE,
                       "CHF", amount * USD_TO_CHF_RATE,
                       "JPY", amount * USD_TO_JPY_RATE)));

        // Act & Assert
        given().
            mockMvc(mockMvc).
            contentType(ContentType.JSON).
            param("from", baseCurrency).
            param("to", targetCurrencies).
            param("value", amount).
        when().
            get("/api/v1/currency/convert").
        then().
            statusCode(HttpStatus.SC_OK).
            and().
            body("base", is(baseCurrency),
                 "conversions.EUR", is(notNullValue()),
                 "conversions.EUR", is((float) (amount * USD_TO_EUR_RATE)),
                 "conversions.CHF", is((float) (amount * USD_TO_CHF_RATE)),
                 "conversions.JPY", is((float) (amount * USD_TO_JPY_RATE)),
                 "conversions.size()", equalTo(3));
    }

    // static responses from CurrencyService for use in test expectations
    private static final Instant now = Instant.now();
    private static final double USD_TO_EUR_RATE = 2.0;
    private static final double USD_TO_CHF_RATE = 10.0;
    private static final double USD_TO_JPY_RATE = 100.0;
    private static final CurrencyConversionDTO usdToEur = new CurrencyConversionDTO("USD", now, Map.of("EUR", USD_TO_EUR_RATE));
    private static final CurrencyConversionDTO usdToAll = new CurrencyConversionDTO("USD", now, Map.of("EUR", USD_TO_EUR_RATE, "CHF", 10.0, "JPY", 100.0));

}
