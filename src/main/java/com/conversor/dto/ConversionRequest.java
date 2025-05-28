package com.conversor.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ConversionRequest {
    @NotBlank(message = "A moeda de origem é obrigatória")
    private String from;

    @NotBlank(message = "A moeda de destino é obrigatória")
    private String to;

    @NotNull(message = "O valor é obrigatório")
    @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
    private BigDecimal amount;
} 