package com.example.cryptoengine.matching;

import com.example.cryptoengine.domain.OrderSide;
import com.example.cryptoengine.domain.OrderType;
import com.example.cryptoengine.domain.entity.Order;
import com.example.cryptoengine.domain.entity.Trade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Matching engine that executes price-time priority matching.
 * Thread-safe matching logic for LIMIT and MARKET orders.
 * 
 * Matching rules:
 * - Price priority: better prices match first
 * - Time priority: earlier orders at same price match first
 * - Market orders match immediately at best available price
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingEngine {

    private final OrderBookManager orderBookManager;

    /**
     * Matches a new order against the order book.
     * Returns list of trades that were executed.
     * 
     * @param order The order to match
     * @return List of executed trades (empty if no matches)
     */
    public List<Trade> matchOrder(Order order) {
        List<Trade> executedTrades = new ArrayList<>();
        
        if (order.getType() == OrderType.MARKET) {
            return matchMarketOrder(order);
        } else {
            return matchLimitOrder(order);
        }
    }

    /**
     * Matches a MARKET order immediately at best available price.
     */
    private List<Trade> matchMarketOrder(Order order) {
        List<Trade> trades = new ArrayList<>();
        OrderBook orderBook = orderBookManager.getOrderBook(order.getSymbol());
        
        BigDecimal remainingQuantity = order.getRemainingQuantity();
        
        if (order.getSide() == OrderSide.BUY) {
            // Market BUY: match against asks (sell orders)
            while (remainingQuantity.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal bestAskPrice = orderBook.getBestAskPrice();
                if (bestAskPrice == null) {
                    log.warn("Market BUY order {} cannot be filled: no asks available", order.getId());
                    break;
                }
                
                List<OrderBookEntry> matchingAsks = orderBook.getMatchingAsks(bestAskPrice);
                if (matchingAsks.isEmpty()) {
                    break;
                }
                
                for (OrderBookEntry askEntry : matchingAsks) {
                    if (remainingQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                        break;
                    }
                    
                    BigDecimal fillQuantity = remainingQuantity.min(askEntry.getQuantity());
                    BigDecimal fillPrice = askEntry.getPrice();
                    
                    Trade trade = createTrade(order.getId(), askEntry.getOrderId(), fillPrice, fillQuantity, order);
                    trades.add(trade);
                    
                    remainingQuantity = remainingQuantity.subtract(fillQuantity);
                    log.info("Market BUY order {} matched: {} @ {}", order.getId(), fillQuantity, fillPrice);
                }
            }
        } else {
            // Market SELL: match against bids (buy orders)
            while (remainingQuantity.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal bestBidPrice = orderBook.getBestBidPrice();
                if (bestBidPrice == null) {
                    log.warn("Market SELL order {} cannot be filled: no bids available", order.getId());
                    break;
                }
                
                List<OrderBookEntry> matchingBids = orderBook.getMatchingBids(bestBidPrice);
                if (matchingBids.isEmpty()) {
                    break;
                }
                
                for (OrderBookEntry bidEntry : matchingBids) {
                    if (remainingQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                        break;
                    }
                    
                    BigDecimal fillQuantity = remainingQuantity.min(bidEntry.getQuantity());
                    BigDecimal fillPrice = bidEntry.getPrice();
                    
                    Trade trade = createTrade(bidEntry.getOrderId(), order.getId(), fillPrice, fillQuantity, order);
                    trades.add(trade);
                    
                    remainingQuantity = remainingQuantity.subtract(fillQuantity);
                    log.info("Market SELL order {} matched: {} @ {}", order.getId(), fillQuantity, fillPrice);
                }
            }
        }
        
        return trades;
    }

    /**
     * Matches a LIMIT order against the order book.
     * Only matches if price is acceptable.
     */
    private List<Trade> matchLimitOrder(Order order) {
        List<Trade> trades = new ArrayList<>();
        OrderBook orderBook = orderBookManager.getOrderBook(order.getSymbol());
        
        BigDecimal limitPrice = order.getPrice();
        BigDecimal remainingQuantity = order.getRemainingQuantity();
        
        if (order.getSide() == OrderSide.BUY) {
            // Limit BUY: match against asks at or below limit price
            BigDecimal bestAskPrice = orderBook.getBestAskPrice();
            if (bestAskPrice == null || bestAskPrice.compareTo(limitPrice) > 0) {
                // No matching asks, add to order book
                orderBook.addOrder(order);
                log.debug("Limit BUY order {} added to order book at price {}", order.getId(), limitPrice);
                return trades;
            }
            
            List<OrderBookEntry> matchingAsks = orderBook.getMatchingAsks(limitPrice);
            for (OrderBookEntry askEntry : matchingAsks) {
                if (remainingQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
                if (askEntry.getPrice().compareTo(limitPrice) > 0) {
                    break; // Price too high
                }
                
                BigDecimal fillQuantity = remainingQuantity.min(askEntry.getQuantity());
                BigDecimal fillPrice = askEntry.getPrice();
                
                Trade trade = createTrade(order.getId(), askEntry.getOrderId(), fillPrice, fillQuantity, order);
                trades.add(trade);
                
                remainingQuantity = remainingQuantity.subtract(fillQuantity);
                log.info("Limit BUY order {} matched: {} @ {}", order.getId(), fillQuantity, fillPrice);
            }
            
            // If still has remaining quantity, add to order book
            if (remainingQuantity.compareTo(BigDecimal.ZERO) > 0) {
                orderBook.addOrder(order);
            }
        } else {
            // Limit SELL: match against bids at or above limit price
            BigDecimal bestBidPrice = orderBook.getBestBidPrice();
            if (bestBidPrice == null || bestBidPrice.compareTo(limitPrice) < 0) {
                // No matching bids, add to order book
                orderBook.addOrder(order);
                log.debug("Limit SELL order {} added to order book at price {}", order.getId(), limitPrice);
                return trades;
            }
            
            List<OrderBookEntry> matchingBids = orderBook.getMatchingBids(limitPrice);
            for (OrderBookEntry bidEntry : matchingBids) {
                if (remainingQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
                if (bidEntry.getPrice().compareTo(limitPrice) < 0) {
                    break; // Price too low
                }
                
                BigDecimal fillQuantity = remainingQuantity.min(bidEntry.getQuantity());
                BigDecimal fillPrice = bidEntry.getPrice();
                
                Trade trade = createTrade(bidEntry.getOrderId(), order.getId(), fillPrice, fillQuantity, order);
                trades.add(trade);
                
                remainingQuantity = remainingQuantity.subtract(fillQuantity);
                log.info("Limit SELL order {} matched: {} @ {}", order.getId(), fillQuantity, fillPrice);
            }
            
            // If still has remaining quantity, add to order book
            if (remainingQuantity.compareTo(BigDecimal.ZERO) > 0) {
                orderBook.addOrder(order);
            }
        }
        
        return trades;
    }

    /**
     * Creates a Trade entity from matched orders.
     */
    private Trade createTrade(Long buyOrderId, Long sellOrderId, BigDecimal price, BigDecimal quantity, Order currentOrder) {
        return Trade.builder()
            .orderIdBuy(buyOrderId)
            .orderIdSell(sellOrderId)
            .price(price)
            .quantity(quantity)
            .baseCurrency(currentOrder.getBaseCurrency().name())
            .quoteCurrency(currentOrder.getQuoteCurrency().name())
            .timestamp(LocalDateTime.now())
            .build();
    }
}
