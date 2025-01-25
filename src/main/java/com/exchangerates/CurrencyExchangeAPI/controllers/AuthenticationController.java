package com.exchangerates.CurrencyExchangeAPI.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthenticationController {

    @PostMapping("signup")
    public ResponseEntity<String> signUp() {
        return ResponseEntity.ok("Hello!");
    }

    @GetMapping("login")
    public ResponseEntity<String> login() {
        return ResponseEntity.ok("Login endpoint hit!");
    }

}