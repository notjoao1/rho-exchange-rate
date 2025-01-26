package com.exchangerates.CurrencyExchangeAPI.contracts.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountCredentialsDTO {
    @NotEmpty @Email private String email;

    @NotEmpty private String password;
}
