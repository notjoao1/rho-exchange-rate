package com.exchangerates.CurrencyExchangeAPI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CurrencyService {
    @Autowired
    private final RestTemplate httpClient;

    // TODO: currency conversion from A to B
    // TODO: all exchange rates for currency A
    // TODO: value conversion from currency A to B
    // TODO: value conversion from currency A to a list of currencies

    // the method that fetches external currency rates should handle caching
}
