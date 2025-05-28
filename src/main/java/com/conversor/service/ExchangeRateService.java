package com.conversor.service;

import com.conversor.dto.ConversionResponse;
import com.conversor.model.ConversionHistory;
import com.conversor.repository.ConversionHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import com.conversor.exception.CurrencyNotFoundException;
import com.conversor.exception.ExternalApiException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExchangeRateService {
    private final RestTemplate restTemplate;
    private final ConversionHistoryRepository conversionHistoryRepository;

    @Value("${exchange-rate.api.base-url}")
    private String baseUrl;

    @Value("${exchange-rate.api.provider}")
    private String provider;

    @Cacheable(value = "exchangeRates", key = "#fromCurrency")
    public Map<String, BigDecimal> getExchangeRates(String fromCurrency) {
        String url = baseUrl + "/" + fromCurrency;
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> responseBody = response.getBody();

            if (responseBody == null || !responseBody.containsKey("rates")) {
                throw new ExternalApiException("Resposta inválida da API externa");
            }

            @SuppressWarnings("unchecked")
            Map<String, BigDecimal> rates = (Map<String, BigDecimal>) responseBody.get("rates");

            if (rates == null || rates.isEmpty()) {
                 throw new ExternalApiException("Taxas de câmbio não encontradas na resposta da API externa");
            }

            return rates;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new CurrencyNotFoundException(fromCurrency);
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            throw new ExternalApiException("Erro ao chamar a API externa: " + ex.getStatusCode(), ex);
        } catch (Exception ex) {
            throw new ExternalApiException("Erro desconhecido ao chamar a API externa", ex);
        }
    }

    public ConversionResponse convert(String fromCurrency, String toCurrency, BigDecimal amount) {
        Map<String, BigDecimal> rates = getExchangeRates(fromCurrency);
        BigDecimal rate = rates.get(toCurrency);

        if (rate == null) {
            throw new CurrencyNotFoundException(toCurrency);
        }

        BigDecimal convertedAmount = amount.multiply(rate).setScale(6, RoundingMode.HALF_UP);

        ConversionHistory history = new ConversionHistory(
                null,
                fromCurrency,
                toCurrency,
                amount,
                convertedAmount,
                rate,
                LocalDateTime.now(),
                provider
        );
        conversionHistoryRepository.save(history);

        return new ConversionResponse(
                fromCurrency,
                toCurrency,
                amount,
                convertedAmount,
                rate,
                history.getTimestamp(),
                provider
        );
    }

    public Page<ConversionHistory> getConversionHistory(
            String fromCurrency,
            String toCurrency,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {
        return conversionHistoryRepository.findByFromCurrencyAndToCurrencyAndTimestampBetween(
                fromCurrency,
                toCurrency,
                startDate,
                endDate,
                pageable
        );
    }
} 