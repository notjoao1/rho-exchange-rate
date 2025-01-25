package com.exchangerates.CurrencyExchangeAPI.contracts.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AccountCredentialsDTO {
    @NotNull
    @Email
    private String email;

    @NotNull
    private String password;
}
