package com.exchangerates.CurrencyExchangeAPI.services;

import com.exchangerates.CurrencyExchangeAPI.domain.AvailableCurrenciesResponse;
import com.exchangerates.CurrencyExchangeAPI.domain.CurrencyRatesResponse;
import com.exchangerates.CurrencyExchangeAPI.services.interfaces.ICurrencyAPIClient;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class CurrencyAPIClient implements ICurrencyAPIClient {
    private static final String BASE_EXCHANGERATE_API_URL = "https://api.exchangerate.host";
    private final RestTemplate httpClient;
    private final Logger logger = LoggerFactory.getLogger(CurrencyAPIClient.class);

    @Value("${exchangerate.apikey}")
    private String exchangeRateKey;

    @Autowired
    public CurrencyAPIClient(RestTemplate httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public AvailableCurrenciesResponse fetchAvailableCurrencies() {
        var requestUri =
                UriComponentsBuilder.fromUriString(BASE_EXCHANGERATE_API_URL + "/list")
                        .queryParam("access_key", exchangeRateKey)
                        .toUriString();
        var availableCurrenciesResponse =
                httpClient.getForObject(requestUri, AvailableCurrenciesResponse.class);

        if (!availableCurrenciesResponse.isSuccess()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, // 502 status code
                    availableCurrenciesResponse.getError().getInfo());
        }

        return availableCurrenciesResponse;
    }

    @Override
    public CurrencyRatesResponse fetchCurrencyExchangeRates(
            String baseCurrency, List<String> targetCurrencies) {
        var requestUri =
                UriComponentsBuilder.fromUriString(BASE_EXCHANGERATE_API_URL + "/live")
                        .queryParam("access_key", exchangeRateKey)
                        .queryParam("source", baseCurrency)
                        .queryParam("currencies", String.join(",", targetCurrencies))
                        .toUriString();

        logger.info("GET request to external API at {}.", requestUri);
        var currencyRatesResponse =
                httpClient.getForObject(requestUri, CurrencyRatesResponse.class);

        if (!currencyRatesResponse.isSuccess()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, // 502 status code
                    currencyRatesResponse.getError().getInfo());
        }

        // remove the prefixes
        transformCurrencyResponse(currencyRatesResponse);
        return currencyRatesResponse;
    }

    /**
     * This method's purpose is to change the rates mapping portion of the response,
     * removing the prefix of the source currency from the multiple target currencies.
     * For example, a request from EUR to USD, will return, in the quotes field,
     * {"EURUSD": 1.2}. This method turns that into {"USD": 1.2}
     */
    private void transformCurrencyResponse(CurrencyRatesResponse response) {
        var newQuotes = new HashMap<String, Double>();
        var sourceCurrency = response.getSource();
        for (var currencyExchangePair : response.getQuotes().entrySet()) {
            var targetCurrency = currencyExchangePair.getKey().replace(sourceCurrency, "");
            newQuotes.put(targetCurrency, currencyExchangePair.getValue());
        }

        response.setQuotes(newQuotes);
    }
}
