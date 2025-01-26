package com.exchangerates.CurrencyExchangeAPI.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.exchangerates.CurrencyExchangeAPI.contracts.requests.AccountCredentialsDTO;
import com.exchangerates.CurrencyExchangeAPI.contracts.responses.AccountResponseDTO;
import com.exchangerates.CurrencyExchangeAPI.entities.User;
import com.exchangerates.CurrencyExchangeAPI.exception.BusinessException;
import com.exchangerates.CurrencyExchangeAPI.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {
    @InjectMocks
    AuthenticationService authenticationService;

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    private static final String VALID_EMAIL = "test@example.com";
    private static final String VALID_PASSWORD = "password123";
    private static final String VALID_API_KEY = "test-api-key";
    private static final String ENCODED_PASSWORD = "encoded_password";

    @Test
    void givenValidCredentials_WhenSignUp_ThenSucceed() {
        // Arrange
        var credentials = new AccountCredentialsDTO(VALID_EMAIL, VALID_PASSWORD);
        when(userRepository.existsByEmail(VALID_EMAIL)).thenReturn(false);
        when(passwordEncoder.encode(VALID_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        // Act
        AccountResponseDTO response = authenticationService.signUp(credentials);

        // Assert
        assertEquals(VALID_EMAIL, response.getEmail());
        assertNotNull(response.getApiKey());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void givenExistingEmail_WhenSignUp_ThenThrowException() {
        // Arrange
        var credentials = new AccountCredentialsDTO(VALID_EMAIL, VALID_PASSWORD);
        when(userRepository.existsByEmail(VALID_EMAIL)).thenReturn(true);

        // Act & Assert
        assertThrows(BusinessException.class, () -> authenticationService.signUp(credentials));
    }

    @Test
    void givenValidCredentials_WhenGetAccountInfo_ThenSucceed() {
        // Arrange
        var credentials = new AccountCredentialsDTO(VALID_EMAIL, VALID_PASSWORD);
        var user = createValidUser();
        when(userRepository.findByEmail(VALID_EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(VALID_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

        // Act
        AccountResponseDTO response = authenticationService.getAccountInformation(credentials);

        // Assert
        assertEquals(VALID_EMAIL, response.getEmail());
        assertEquals(VALID_API_KEY, response.getApiKey());
    }

    @Test
    void givenInvalidEmail_WhenGetAccountInfo_ThenThrowException() {
        // Arrange
        var credentials = new AccountCredentialsDTO(VALID_EMAIL, VALID_PASSWORD);
        when(userRepository.findByEmail(VALID_EMAIL)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BusinessException.class, 
            () -> authenticationService.getAccountInformation(credentials));
    }

    @Test
    void givenInvalidPassword_WhenGetAccountInfo_ThenThrowException() {
        // Arrange
        var credentials = new AccountCredentialsDTO(VALID_EMAIL, "wrong_password");
        var user = createValidUser();
        when(userRepository.findByEmail(VALID_EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong_password", ENCODED_PASSWORD)).thenReturn(false);

        // Act & Assert
        assertThrows(BusinessException.class, 
            () -> authenticationService.getAccountInformation(credentials));
    }

    @Test
    void givenValidApiKey_WhenValidateApiKey_ThenReturnTrue() {
        // Arrange
        when(userRepository.existsByApiKey(VALID_API_KEY)).thenReturn(true);

        // Act & Assert
        assertTrue(authenticationService.isValidAPIKey(VALID_API_KEY));
    }

    @Test
    void givenInvalidApiKey_WhenValidateApiKey_ThenReturnFalse() {
        // Arrange
        when(userRepository.existsByApiKey("invalid_key")).thenReturn(false);

        // Act & Assert
        assertFalse(authenticationService.isValidAPIKey("invalid_key"));
    }

    @Test
    void givenValidCredentials_WhenRevokeApiKey_ThenSucceed() {
        // Arrange
        var credentials = new AccountCredentialsDTO(VALID_EMAIL, VALID_PASSWORD);
        var user = createValidUser();
        when(userRepository.findByEmail(VALID_EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(VALID_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AccountResponseDTO response = authenticationService.revokeAPIKey(credentials);

        // Assert
        assertNotEquals(VALID_API_KEY, response.getApiKey());
        verify(userRepository).save(any(User.class));
    }

    private User createValidUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail(VALID_EMAIL);
        user.setPassword(ENCODED_PASSWORD);
        user.setApiKey(VALID_API_KEY);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }
}