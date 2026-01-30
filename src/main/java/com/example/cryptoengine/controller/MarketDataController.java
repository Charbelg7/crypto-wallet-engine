package com.example.cryptoengine.controller;

import com.example.cryptoengine.application.dto.OrderBookResponse;
import com.example.cryptoengine.application.dto.TradeResponse;
import com.example.cryptoengine.matching.OrderBook;
import com.example.cryptoengine.matching.OrderBookEntry;
import com.example.cryptoengine.matching.OrderBookManager;
import com.example.cryptoengine.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for market data (order book, trades).
 * API version: v1
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/market")
@RequiredArgsConstructor
public class MarketDataController {

    private final OrderBookManager orderBookManager;
    private final MarketDataService marketDataService;

    @GetMapping("/orderbook/{symbol}")
    public ResponseEntity<OrderBookResponse> getOrderBook(@PathVariable String symbol) {
        OrderBook orderBook = orderBookManager.getOrderBook(symbol);
        Map<String, List<OrderBookEntry>> snapshot = orderBook.getSnapshot();

        List<OrderBookResponse.OrderBookLevel> bids = snapshot.get("bids").stream()
            .map(entry -> new OrderBookResponse.OrderBookLevel(entry.getPrice(), entry.getQuantity()))
            .collect(Collectors.toList());

        List<OrderBookResponse.OrderBookLevel> asks = snapshot.get("asks").stream()
            .map(entry -> new OrderBookResponse.OrderBookLevel(entry.getPrice(), entry.getQuantity()))
            .collect(Collectors.toList());

        OrderBookResponse response = new OrderBookResponse(symbol, bids, asks);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/trades/{symbol}")
    public ResponseEntity<List<TradeResponse>> getTrades(
            @PathVariable String symbol,
            @RequestParam(required = false, defaultValue = "100") int limit) {
        List<TradeResponse> trades = marketDataService.getRecentTrades(symbol, limit);
        return ResponseEntity.ok(trades);
    }
}
