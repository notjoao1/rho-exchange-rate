package com.exchangerates.CurrencyExchangeAPI.contracts.responses;

import com.exchangerates.CurrencyExchangeAPI.entities.User;
import lombok.Data;

@Data
public class AccountResponseDTO {
    private long id;
    private String email;
    private String apiKey;

    public static AccountResponseDTO fromUser(User user) {
        AccountResponseDTO accountResponseDTO = new AccountResponseDTO();
        accountResponseDTO.setId(user.getId());
        accountResponseDTO.setEmail(user.getEmail());
        accountResponseDTO.setApiKey(user.getApiKey());
        return accountResponseDTO;
    }
}
