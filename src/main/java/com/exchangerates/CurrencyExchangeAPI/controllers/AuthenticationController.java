package com.exchangerates.CurrencyExchangeAPI.controllers;

import com.exchangerates.CurrencyExchangeAPI.contracts.requests.AccountCredentialsDTO;
import com.exchangerates.CurrencyExchangeAPI.contracts.responses.AccountResponseDTO;
import com.exchangerates.CurrencyExchangeAPI.services.interfaces.IAuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Authentication and Account Management",
        description =
                "Endpoints related to authentication, account management and API access through API"
                        + " keys.")
@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthenticationController {

    private final IAuthenticationService authenticationService;

    @Operation(
            summary = "Register new user",
            description = "Creates a new user account and returns the generated API key")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "201",
                        description = "User successfully created",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema =
                                                @Schema(
                                                        implementation =
                                                                AccountResponseDTO.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid input credentials or email already registered."),
            })
    @PostMapping("signup")
    public ResponseEntity<AccountResponseDTO> signUp(
            @Valid @RequestBody AccountCredentialsDTO signUpDTO) {
        var createdUser = authenticationService.signUp(signUpDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @Operation(
            summary = "Get account information",
            description =
                    "Retrieves current user account information using email/password"
                            + " authentication")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Account information retrieved successfully"),
                @ApiResponse(responseCode = "400", description = "Invalid credentials")
            })
    @GetMapping("info")
    public ResponseEntity<AccountResponseDTO> getAccountInformation(
            @Valid @RequestBody AccountCredentialsDTO loginDTO) {
        var requestedUser = authenticationService.getAccountInformation(loginDTO);
        return ResponseEntity.status(HttpStatus.OK).body(requestedUser);
    }

    @Operation(
            summary = "Revoke current API key and generate new API key",
            description = "Invalidates current API key and generates a new one")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "201",
                        description = "New API key generated successfully"),
                @ApiResponse(responseCode = "400", description = "Invalid credentials")
            })
    @PutMapping("revoke-apikey")
    public ResponseEntity<AccountResponseDTO> revokeAPIKey(
            @Valid @RequestBody AccountCredentialsDTO loginDTO) {
        var updatedUser = authenticationService.revokeAPIKey(loginDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(updatedUser);
    }
}
