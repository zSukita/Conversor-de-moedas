package com.conversor.exception;

public class CurrencyNotFoundException extends RuntimeException {
    public CurrencyNotFoundException(String currencyCode) {
        super("Moeda não encontrada ou não suportada: " + currencyCode);
    }
} 