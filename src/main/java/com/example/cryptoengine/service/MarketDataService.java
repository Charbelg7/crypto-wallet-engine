package com.example.cryptoengine.service;

import com.example.cryptoengine.application.dto.TradeResponse;
import com.example.cryptoengine.domain.entity.Trade;
import com.example.cryptoengine.infrastructure.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for market data queries.
 */
@Service
@RequiredArgsConstructor
public class MarketDataService {

    private final TradeRepository tradeRepository;

    public List<TradeResponse> getRecentTrades(String symbol, int limit) {
        String[] parts = symbol.split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid symbol format: " + symbol);
        }

        List<Trade> trades = tradeRepository.findBySymbol(parts[0], parts[1]);
        
        return trades.stream()
            .limit(limit)
            .map(t -> new TradeResponse(
                t.getId(),
                t.getOrderIdBuy(),
                t.getOrderIdSell(),
                t.getPrice(),
                t.getQuantity(),
                t.getBaseCurrency(),
                t.getQuoteCurrency(),
                t.getBaseCurrency() + "/" + t.getQuoteCurrency(),
                t.getTimestamp()
            ))
            .collect(Collectors.toList());
    }
}
