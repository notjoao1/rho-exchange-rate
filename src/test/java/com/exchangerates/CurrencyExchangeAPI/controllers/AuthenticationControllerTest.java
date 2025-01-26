package com.exchangerates.CurrencyExchangeAPI.controllers;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.exchangerates.CurrencyExchangeAPI.contracts.requests.AccountCredentialsDTO;
import com.exchangerates.CurrencyExchangeAPI.contracts.responses.AccountResponseDTO;
import com.exchangerates.CurrencyExchangeAPI.exception.BusinessException;
import com.exchangerates.CurrencyExchangeAPI.services.interfaces.IAuthenticationService;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false) // not testing for security here
class AuthenticationControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean IAuthenticationService authService;

    private static final long VALID_USER_ID = 1001;
    private static final String VALID_EMAIL = "test@example.com";
    private static final String VALID_PASSWORD = "password123";
    private static final String VALID_API_KEY = "test-api-key";

    @Test
    void givenValidCredentials_WhenSignUp_ThenReturnCreated() {
        var credentials = new AccountCredentialsDTO(VALID_EMAIL, VALID_PASSWORD);
        var response = new AccountResponseDTO(VALID_USER_ID, VALID_EMAIL, VALID_API_KEY);

        when(authService.signUp(any(AccountCredentialsDTO.class))).thenReturn(response);

        given().
            mockMvc(mockMvc).
            contentType(ContentType.JSON).
            body(credentials).
        when().
            post("/api/v1/auth/signup").
        then().
            statusCode(201).
            and().
            body("email", equalTo(VALID_EMAIL), "apiKey", equalTo(VALID_API_KEY));
    }

    @Test
    void givenEmptyEmail_SignUp_ShouldReturnBadRequest() {
        var credentials = new AccountCredentialsDTO("", VALID_PASSWORD);

        given().
            mockMvc(mockMvc).
            contentType(ContentType.JSON).
            body(credentials).
        when().
            post("/api/v1/auth/signup").
        then().
            statusCode(400).
            and().
            body("message", is(notNullValue()));
    }

    @Test
    void givenEmptyPassword_WhenSignUp_ShouldReturnBadRequest() {
        var credentials = new AccountCredentialsDTO(VALID_EMAIL, "");

        given().
            mockMvc(mockMvc).
            contentType(ContentType.JSON).
            body(credentials).
        when().
            post("/api/v1/auth/signup").
        then().
            statusCode(400).
            and().
            body("message", is(notNullValue()));
    }

    @Test
    void givenInvalidEmailFormat_WhenSignUp_ShouldReturnBadRequest() {
        var credentials = new AccountCredentialsDTO("not-gonna-work.com", VALID_PASSWORD);

        given().
            mockMvc(mockMvc).
            contentType(ContentType.JSON).
            body(credentials).
        when().
            post("/api/v1/auth/signup").
        then().
            statusCode(400).
            and().
            body("message", is(notNullValue()));
    }

    @Test
    void givenValidCredentials_WhenGetInfo_ThenReturnOk() {
        var credentials = new AccountCredentialsDTO(VALID_EMAIL, VALID_PASSWORD);
        var response = new AccountResponseDTO(VALID_USER_ID, VALID_EMAIL, VALID_API_KEY);

        when(authService.getAccountInformation(any(AccountCredentialsDTO.class)))
                .thenReturn(response);

        given().
            mockMvc(mockMvc).
            contentType(ContentType.JSON).
            body(credentials).
        when().
            get("/api/v1/auth/info").
        then().
            statusCode(200).
            body(
            "email", equalTo(VALID_EMAIL),
            "apiKey", equalTo(VALID_API_KEY)
            );
    }

    @Test
    void givenInvalidCredentials_WhenGetInfo_ThenReturnBadRequest() {
        var credentials = new AccountCredentialsDTO(VALID_EMAIL, "not-the-right-password");

        when(authService.getAccountInformation(any(AccountCredentialsDTO.class)))
                .thenThrow(new BusinessException("Invalid credentials"));

        given().
            mockMvc(mockMvc).
            contentType(ContentType.JSON).
            body(credentials).
        when().
            get("/api/v1/auth/info").
        then().
            statusCode(400).
            body("message", is(notNullValue()));
    }

    @Test
    void givenValidCredentials_WhenRevokeApiKey_ThenReturnCreated() {
        var credentials = new AccountCredentialsDTO(VALID_EMAIL, VALID_PASSWORD);
        var response = new AccountResponseDTO(VALID_USER_ID, VALID_EMAIL, "new-api-key");

        when(authService.revokeAPIKey(any(AccountCredentialsDTO.class))).thenReturn(response);

        given().
            mockMvc(mockMvc).
            contentType(ContentType.JSON).
            body(credentials).
        when().
            put("/api/v1/auth/revoke-apikey").
        then().
            statusCode(201).
            body("email", equalTo(VALID_EMAIL)).
            body("apiKey", not(equalTo(VALID_API_KEY)));
    }

    @Test
    void givenInvalidCredentials_WhenRevokeApiKey_ThenReturnBadRequest() {
        var credentials = new AccountCredentialsDTO(VALID_EMAIL, "not-the-right-password");

        when(authService.revokeAPIKey(any(AccountCredentialsDTO.class)))
                .thenThrow(new BusinessException("Invalid credentials"));

        given().
            mockMvc(mockMvc).
            contentType(ContentType.JSON).
            body(credentials).
        when().
            put("/api/v1/auth/revoke-apikey").
        then().
            statusCode(400).
            body("message", is(notNullValue()));
    }
}
