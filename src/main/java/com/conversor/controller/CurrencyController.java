package com.conversor.controller;

import com.conversor.dto.ConversionResponse;
import com.conversor.model.ConversionHistory;
import com.conversor.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/currencies")
@RequiredArgsConstructor
@Tag(name = "Currency Converter", description = "API para conversão de moedas")
public class CurrencyController {
    private final ExchangeRateService exchangeRateService;

    @GetMapping
    @Operation(summary = "Listar todas as moedas suportadas",
               description = "Retorna a lista de moedas suportadas e suas taxas de câmbio em relação a uma moeda base (atualmente USD).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Operação bem-sucedida",
                         content = @Content(mediaType = "application/json",
                         schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "503", description = "Serviço externo indisponível",
                         content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<Map<String, BigDecimal>> listCurrencies() {
        // Atualmente, lista as taxas em relação ao USD. Pode ser melhorado para listar apenas moedas suportadas.
        return ResponseEntity.ok(exchangeRateService.getExchangeRates("USD"));
    }

    @GetMapping("/convert")
    @Operation(summary = "Converter valor entre moedas",
               description = "Converte um valor de uma moeda para outra utilizando taxas de câmbio em tempo real.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Conversão bem-sucedida",
                         content = @Content(mediaType = "application/json",
                         schema = @Schema(implementation = ConversionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Requisição inválida (moeda não suportada, valor inválido)",
                         content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "429", description = "Rate limit excedido",
                         content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "503", description = "Serviço externo indisponível",
                         content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<ConversionResponse> convert(
            @Parameter(description = "Código da moeda de origem (ISO 4217)", example = "USD")
            @RequestParam String from,
            @Parameter(description = "Código da moeda de destino (ISO 4217)", example = "BRL")
            @RequestParam String to,
            @Parameter(description = "Valor a ser convertido", example = "100.50")
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(exchangeRateService.convert(from, to, amount));
    }

    @GetMapping("/rates/{currency}")
    @Operation(summary = "Obter taxas de câmbio de uma moeda base",
               description = "Retorna as taxas de câmbio de uma moeda base em relação a outras moedas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Operação bem-sucedida",
                         content = @Content(mediaType = "application/json",
                         schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "Requisição inválida (moeda base não suportada)",
                         content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "503", description = "Serviço externo indisponível",
                         content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<Map<String, BigDecimal>> getRates(
            @Parameter(description = "Código da moeda base (ISO 4217)", example = "EUR")
            @PathVariable String currency) {
        return ResponseEntity.ok(exchangeRateService.getExchangeRates(currency));
    }

    @GetMapping("/history/{fromCurrency}/{toCurrency}")
    @Operation(summary = "Obter histórico de conversões",
               description = "Retorna o histórico de conversões entre duas moedas em um determinado período.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Histórico retornado com sucesso",
                         content = @Content(mediaType = "application/json",
                         schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", description = "Requisição inválida (formato de data incorreto)",
                         content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Erro interno do servidor",
                         content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<Page<ConversionHistory>> getHistory(
            @Parameter(description = "Código da moeda de origem (ISO 4217)", example = "USD")
            @PathVariable String fromCurrency,
            @Parameter(description = "Código da moeda de destino (ISO 4217)", example = "BRL")
            @PathVariable String toCurrency,
            @Parameter(description = "Data de início do período (ISO 8601)", example = "2023-01-01T00:00:00Z")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Data de fim do período (ISO 8601)", example = "2023-12-31T23:59:59Z")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Pageable pageable) {
        return ResponseEntity.ok(exchangeRateService.getConversionHistory(fromCurrency, toCurrency, startDate, endDate, pageable));
    }
} 