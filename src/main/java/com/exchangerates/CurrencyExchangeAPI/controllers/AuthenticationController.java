package com.exchangerates.CurrencyExchangeAPI.controllers;

import com.exchangerates.CurrencyExchangeAPI.contracts.requests.AccountCredentialsDTO;
import com.exchangerates.CurrencyExchangeAPI.contracts.responses.AccountResponseDTO;
import com.exchangerates.CurrencyExchangeAPI.services.interfaces.IAuthenticationService;
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

@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthenticationController {

    private final IAuthenticationService authenticationService;

    @PostMapping("signup")
    public ResponseEntity<AccountResponseDTO> signUp(
            @Valid @RequestBody AccountCredentialsDTO signUpDTO) {
        var createdUser = authenticationService.signUp(signUpDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @GetMapping("info")
    public ResponseEntity<AccountResponseDTO> getAccountInformation(
            @Valid @RequestBody AccountCredentialsDTO loginDTO) {
        var requestedUser = authenticationService.getAccountInformation(loginDTO);
        return ResponseEntity.status(HttpStatus.OK).body(requestedUser);
    }

    @PutMapping("revoke-apikey")
    public ResponseEntity<AccountResponseDTO> revokeAPIKey(
            @Valid @RequestBody AccountCredentialsDTO loginDTO) {
        var updatedUser = authenticationService.revokeAPIKey(loginDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(updatedUser);
    }
}
