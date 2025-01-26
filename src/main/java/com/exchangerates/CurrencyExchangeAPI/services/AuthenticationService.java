package com.exchangerates.CurrencyExchangeAPI.services;

import com.exchangerates.CurrencyExchangeAPI.contracts.requests.AccountCredentialsDTO;
import com.exchangerates.CurrencyExchangeAPI.contracts.responses.AccountResponseDTO;
import com.exchangerates.CurrencyExchangeAPI.entities.User;
import com.exchangerates.CurrencyExchangeAPI.exception.BusinessException;
import com.exchangerates.CurrencyExchangeAPI.repository.UserRepository;
import com.exchangerates.CurrencyExchangeAPI.services.interfaces.IAuthenticationService;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthenticationService implements IAuthenticationService {

    private final UserRepository userRepository;

    private static final SecureRandom rng = new SecureRandom();

    private final PasswordEncoder passwordEncoder;

    @Override
    public AccountResponseDTO signUp(AccountCredentialsDTO request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("User with that email already exists");
        }

        System.out.println(request.getEmail());
        System.out.println(request.getPassword());

        var newUser = new User();
        newUser.setEmail(request.getEmail());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setApiKey(generateAPIKey());
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());
        var savedUser = userRepository.save(newUser);

        return AccountResponseDTO.fromUser(savedUser);
    }

    @Override
    public AccountResponseDTO getAccountInformation(AccountCredentialsDTO request) {
        return AccountResponseDTO.fromUser(checkAuthenticationAndReturnUser(request));
    }

    @Override
    public boolean isValidAPIKey(String apiKey) {
        return userRepository.existsByApiKey(apiKey);
    }

    @Override
    public AccountResponseDTO revokeAPIKey(AccountCredentialsDTO request) {
        var existingUser = checkAuthenticationAndReturnUser(request);
        existingUser.setApiKey(generateAPIKey());
        var updatedUser = userRepository.save(existingUser);

        return AccountResponseDTO.fromUser(updatedUser);
    }

    private User checkAuthenticationAndReturnUser(AccountCredentialsDTO request) {
        var existingUser =
                userRepository
                        .findByEmail(request.getEmail())
                        .orElseThrow(() -> new BusinessException("Invalid credentials."));

        if (!passwordEncoder.matches(request.getPassword(), existingUser.getPassword())) {
            throw new BusinessException("Invalid credentials.");
        }

        return existingUser;
    }

    private String generateAPIKey() {
        // use cryptographically secure rng to generate a random 32 byte sequence
        byte[] apiKey = new byte[32];
        rng.nextBytes(apiKey);
        return Base64.getEncoder().encodeToString(apiKey);
    }
}
