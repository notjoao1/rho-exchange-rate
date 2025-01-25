package com.exchangerates.CurrencyExchangeAPI.services.interfaces;

import com.exchangerates.CurrencyExchangeAPI.contracts.requests.AccountCredentialsDTO;
import com.exchangerates.CurrencyExchangeAPI.contracts.responses.AccountResponseDTO;

public interface IAuthenticationService {
    /**
     * Signs up a new user in the system
     * @param request The request containing the new user's credentials
     * @return The account information for the new user
     */
    public AccountResponseDTO signUp(AccountCredentialsDTO request);

    /**
     * Retrieves the account information for the given user
     * @param request The request containing the user's credentials
     * @return The account information for the given user
     */
    public AccountResponseDTO getAccountInformation(AccountCredentialsDTO request);

    /**
     * Validates whether the provided API key is valid in the system
     * @param apiKey The API key to validate
     * @return True if the API key is valid, false otherwise
     */
    public boolean isValidAPIKey(String apiKey);

    /**
     * Revokes the API key for the given user
     * @param request The request containing the user's credentials
     * @return The account information for the given user, including the new API key
     */
    public AccountResponseDTO revokeAPIKey(AccountCredentialsDTO request);
}
