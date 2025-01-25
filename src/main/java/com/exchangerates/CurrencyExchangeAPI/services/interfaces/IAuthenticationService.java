package com.exchangerates.CurrencyExchangeAPI.services.interfaces;

import com.exchangerates.CurrencyExchangeAPI.contracts.requests.SignUpDTO;

public interface IAuthenticationService {
    /**
     * Signs up a new user in the system
     * @param request The request containing the new user's credentials
     */
    public void signUp(SignUpDTO request);
    /**
     * Validates whether the provided API key is valid in the system
     * @param apiKey The API key to validate
     * @return True if the API key is valid, false otherwise
     */
    public boolean validateAPIKey(String apiKey);
}
