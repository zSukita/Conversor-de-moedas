package com.conversor.repository;

import com.conversor.model.ConversionHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ConversionHistoryRepository extends JpaRepository<ConversionHistory, Long> {
    Page<ConversionHistory> findByFromCurrencyAndToCurrencyAndTimestampBetween(
            String fromCurrency,
            String toCurrency,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );
} 