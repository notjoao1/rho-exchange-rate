package com.exchangerates.CurrencyExchangeAPI.repository;

import com.exchangerates.CurrencyExchangeAPI.entities.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    public Optional<User> findByEmail(String email);

    public boolean existsByApiKey(String apiKey);

    public boolean existsByEmail(String email);
}
