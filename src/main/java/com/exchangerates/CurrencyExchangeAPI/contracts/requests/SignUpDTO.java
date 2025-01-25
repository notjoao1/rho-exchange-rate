package com.exchangerates.CurrencyExchangeAPI.contracts.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public class SignUpDTO {
    @NotNull
    @Email
    public String email;

    @NotNull
    public String password;
}
