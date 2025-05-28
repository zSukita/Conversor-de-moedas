package com.conversor.service;

import com.conversor.dto.ConversionResponse;
import com.conversor.exception.CurrencyNotFoundException;
import com.conversor.exception.ExternalApiException;
import com.conversor.model.ConversionHistory;
import com.conversor.repository.ConversionHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExchangeRateServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ConversionHistoryRepository conversionHistoryRepository;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(exchangeRateService, "baseUrl", "http://api.exchangerate-api.com/v4/latest");
        ReflectionTestUtils.setField(exchangeRateService, "provider", "exchangerate-api");
    }

    @Test
    @SuppressWarnings("unchecked")
    void getExchangeRates_success() {
        Map<String, Object> mockResponseMap = new HashMap<>();
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("USD", BigDecimal.ONE);
        rates.put("BRL", new BigDecimal("5.0"));
        mockResponseMap.put("rates", rates);

        ResponseEntity<Map<String, Object>> responseEntity = new ResponseEntity<>(mockResponseMap, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        Map<String, BigDecimal> resultRates = exchangeRateService.getExchangeRates("USD");

        assertNotNull(resultRates);
        assertEquals(2, resultRates.size());
        assertEquals(BigDecimal.ONE, resultRates.get("USD"));
        assertEquals(new BigDecimal("5.0"), resultRates.get("BRL"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getExchangeRates_currencyNotFound() {
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        assertThrows(CurrencyNotFoundException.class, () -> exchangeRateService.getExchangeRates("INVALID"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getExchangeRates_externalApiError() {
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(ExternalApiException.class, () -> exchangeRateService.getExchangeRates("USD"));
    }

    @Test
    void convert_success() {
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("USD", BigDecimal.ONE);
        rates.put("BRL", new BigDecimal("5.0"));

        ExchangeRateService spyService = spy(exchangeRateService);
        doReturn(rates).when(spyService).getExchangeRates(anyString());

        BigDecimal amount = new BigDecimal("100");
        ConversionResponse response = spyService.convert("USD", "BRL", amount);

        assertNotNull(response);
        assertEquals("USD", response.getFrom());
        assertEquals("BRL", response.getTo());
        assertEquals(amount, response.getAmount());
        assertEquals(new BigDecimal("500.000000"), response.getConvertedAmount());
        assertEquals(new BigDecimal("5.0"), response.getExchangeRate());
        assertNotNull(response.getTimestamp());
        assertEquals("exchangerate-api", response.getProvider());

        verify(conversionHistoryRepository, times(1)).save(any(ConversionHistory.class));
    }

    @Test
    void convert_currencyNotFound() {
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("USD", BigDecimal.ONE);

        ExchangeRateService spyService = spy(exchangeRateService);
        doReturn(rates).when(spyService).getExchangeRates(anyString());

        BigDecimal amount = new BigDecimal("100");

        assertThrows(CurrencyNotFoundException.class, () -> spyService.convert("USD", "EUR", amount));

        verify(conversionHistoryRepository, times(0)).save(any(ConversionHistory.class));
    }

    @Test
    void getConversionHistory_shouldReturnPagedHistory() {
        String fromCurrency = "USD";
        String toCurrency = "BRL";
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, 10);

        List<ConversionHistory> historyList = Arrays.asList(
                new ConversionHistory(1L, fromCurrency, toCurrency, BigDecimal.ONE, new BigDecimal("5.0"), new BigDecimal("5.0"), LocalDateTime.now().minusDays(6), "provider1"),
                new ConversionHistory(2L, fromCurrency, toCurrency, BigDecimal.TEN, new BigDecimal("50.0"), new BigDecimal("5.0"), LocalDateTime.now().minusDays(1), "provider1")
        );
        Page<ConversionHistory> mockPage = new PageImpl<>(historyList, pageable, historyList.size());

        when(conversionHistoryRepository.findByFromCurrencyAndToCurrencyAndTimestampBetween(
                eq(fromCurrency),
                eq(toCurrency),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(pageable)
        )).thenReturn(mockPage);

        Page<ConversionHistory> resultPage = exchangeRateService.getConversionHistory(
                fromCurrency,
                toCurrency,
                startDate,
                endDate,
                pageable
        );

        assertNotNull(resultPage);
        assertEquals(historyList.size(), resultPage.getContent().size());
        assertEquals(historyList.size(), resultPage.getTotalElements());
        verify(conversionHistoryRepository, times(1)).findByFromCurrencyAndToCurrencyAndTimestampBetween(
                eq(fromCurrency),
                eq(toCurrency),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(pageable)
        );
    }

    @Test
    void getConversionHistory_shouldReturnEmptyPageWhenNoHistory() {
        String fromCurrency = "EUR";
        String toCurrency = "GBP";
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, 10);

        Page<ConversionHistory> mockPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(conversionHistoryRepository.findByFromCurrencyAndToCurrencyAndTimestampBetween(
                eq(fromCurrency),
                eq(toCurrency),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(pageable)
        )).thenReturn(mockPage);

        Page<ConversionHistory> resultPage = exchangeRateService.getConversionHistory(
                fromCurrency,
                toCurrency,
                startDate,
                endDate,
                pageable
        );

        assertNotNull(resultPage);
        assertTrue(resultPage.getContent().isEmpty());
        assertEquals(0, resultPage.getTotalElements());
        verify(conversionHistoryRepository, times(1)).findByFromCurrencyAndToCurrencyAndTimestampBetween(
                eq(fromCurrency),
                eq(toCurrency),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(pageable)
        );
    }
} 