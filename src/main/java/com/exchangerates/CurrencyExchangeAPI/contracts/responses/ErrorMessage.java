package com.exchangerates.CurrencyExchangeAPI.contracts.responses;

import lombok.Data;
import lombok.NonNull;

@Data
public class ErrorMessage {
    @NonNull private String message;
}
