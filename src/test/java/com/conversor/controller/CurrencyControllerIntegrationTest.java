package com.conversor.controller;

import com.conversor.dto.ConversionResponse;
import com.conversor.model.ConversionHistory;
import com.conversor.repository.ConversionHistoryRepository;
import com.conversor.service.ExchangeRateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CurrencyControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean // Usamos MockBean para simular o serviço externo
    private ExchangeRateService exchangeRateService;

    @Autowired
    private ConversionHistoryRepository conversionHistoryRepository;

    @BeforeEach
    void setUp() {
        // Limpar o banco de dados H2 em memória antes de cada teste
        conversionHistoryRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        // Limpar o banco de dados H2 em memória depois de cada teste
        conversionHistoryRepository.deleteAll();
    }

    @Test
    void listCurrencies_shouldReturnRates() throws Exception {
        Map<String, BigDecimal> mockRates = new HashMap<>();
        mockRates.put("USD", BigDecimal.ONE);
        mockRates.put("BRL", new BigDecimal("5.0"));

        when(exchangeRateService.getExchangeRates("USD")).thenReturn(mockRates);

        mockMvc.perform(get("/currencies")
                       .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.USD").value(1.0))
               .andExpect(jsonPath("$.BRL").value(5.0));
    }

    @Test
    void convert_shouldReturnConvertedAmount() throws Exception {
        String from = "USD";
        String to = "BRL";
        BigDecimal amount = new BigDecimal("100");
        BigDecimal convertedAmount = new BigDecimal("500");
        BigDecimal rate = new BigDecimal("5.0");

        ConversionResponse mockResponse = new ConversionResponse(
                from, to, amount, convertedAmount, rate, LocalDateTime.now(), "mock-provider"
        );

        when(exchangeRateService.convert(eq(from), eq(to), eq(amount))).thenReturn(mockResponse);

        mockMvc.perform(get("/currencies/convert")
                       .param("from", from)
                       .param("to", to)
                       .param("amount", amount.toString())
                       .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.from").value(from))
               .andExpect(jsonPath("$.to").value(to))
               .andExpect(jsonPath("$.amount").value(amount.doubleValue()))
               .andExpect(jsonPath("$.convertedAmount").value(convertedAmount.doubleValue()))
               .andExpect(jsonPath("$.exchangeRate").value(rate.doubleValue()))
               .andExpect(jsonPath("$.provider").value("mock-provider"));
    }

    @Test
    void getRates_shouldReturnRatesForCurrency() throws Exception {
        String currency = "EUR";
        Map<String, BigDecimal> mockRates = new HashMap<>();
        mockRates.put(currency, BigDecimal.ONE);
        mockRates.put("USD", new BigDecimal("1.1"));

        when(exchangeRateService.getExchangeRates(currency)).thenReturn(mockRates);

        mockMvc.perform(get("/currencies/rates/{currency}", currency)
                       .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.EUR").value(1.0))
               .andExpect(jsonPath("$.USD").value(1.1));
    }

    @Test
    void getHistory_shouldReturnPagedHistory() throws Exception {
        String from = "USD";
        String to = "BRL";
        LocalDateTime now = LocalDateTime.now();

        ConversionHistory history1 = new ConversionHistory(1L, from, to, BigDecimal.ONE, new BigDecimal("5.0"), new BigDecimal("5.0"), now.minusDays(1), "provider1");
        ConversionHistory history2 = new ConversionHistory(2L, from, to, BigDecimal.TEN, new BigDecimal("50.0"), new BigDecimal("5.0"), now, "provider1");

        conversionHistoryRepository.saveAll(Arrays.asList(history1, history2));

        Pageable pageable = PageRequest.of(0, 10);
        PageImpl<ConversionHistory> mockPage = new PageImpl<>(Arrays.asList(history1, history2), pageable, 2);

        when(exchangeRateService.getConversionHistory(eq(from), eq(to), any(LocalDateTime.class), any(LocalDateTime.class), eq(pageable)))
                .thenReturn(mockPage);

        mockMvc.perform(get("/currencies/history/{fromCurrency}/{toCurrency}", from, to)
                       .param("startDate", now.minusDays(2).toString())
                       .param("endDate", now.plusDays(1).toString())
                       .param("page", "0")
                       .param("size", "10")
                       .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.content").isArray())
               .andExpect(jsonPath("$.content.length()").value(2))
               .andExpect(jsonPath("$.totalElements").value(2))
               .andExpect(jsonPath("$.content[0].fromCurrency").value(from))
               .andExpect(jsonPath("$.content[1].toCurrency").value(to));
    }

    @Test
    void getHistory_shouldReturnEmptyPageWhenNoHistory() throws Exception {
         String from = "USD";
         String to = "EUR";
         LocalDateTime now = LocalDateTime.now();

         Pageable pageable = PageRequest.of(0, 10);
         PageImpl<ConversionHistory> mockPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(exchangeRateService.getConversionHistory(eq(from), eq(to), any(LocalDateTime.class), any(LocalDateTime.class), eq(pageable)))
                .thenReturn(mockPage);

        mockMvc.perform(get("/currencies/history/{fromCurrency}/{toCurrency}", from, to)
                       .param("startDate", now.minusDays(2).toString())
                       .param("endDate", now.plusDays(1).toString())
                       .param("page", "0")
                       .param("size", "10")
                       .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.content").isArray())
               .andExpect(jsonPath("$.content.length()").value(0))
               .andExpect(jsonPath("$.totalElements").value(0));
    }
} 