package com.exchangerates.CurrencyExchangeAPI.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "user_table", indexes = @Index(columnList = "apiKey", name = "IX_user_apiKey"))
@Getter
@Setter
@RequiredArgsConstructor
public class User {
    @Id @GeneratedValue private Long id;

    @Email
    @Column(unique = true)
    private String email;

    private String password; // hashed
    private String apiKey;

    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp private LocalDateTime updatedAt;
}
