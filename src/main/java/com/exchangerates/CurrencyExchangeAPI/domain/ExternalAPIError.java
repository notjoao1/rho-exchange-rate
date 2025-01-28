package com.exchangerates.CurrencyExchangeAPI.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExternalAPIError {
    private int code;
    private String info;
}
