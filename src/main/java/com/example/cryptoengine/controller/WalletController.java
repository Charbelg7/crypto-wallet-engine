package com.example.cryptoengine.controller;

import com.example.cryptoengine.application.dto.DepositRequest;
import com.example.cryptoengine.application.dto.WalletBalanceResponse;
import com.example.cryptoengine.domain.Currency;
import com.example.cryptoengine.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for wallet operations.
 * API version: v1
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/deposit")
    public ResponseEntity<WalletBalanceResponse> deposit(
            @RequestParam Long userId,
            @RequestParam Currency currency,
            @Valid @RequestBody DepositRequest request) {
        WalletBalanceResponse balance = walletService.deposit(userId, currency, request);
        return ResponseEntity.ok(balance);
    }

    @GetMapping("/balances")
    public ResponseEntity<List<WalletBalanceResponse>> getBalances(@RequestParam Long userId) {
        List<WalletBalanceResponse> balances = walletService.getBalances(userId);
        return ResponseEntity.ok(balances);
    }

    @GetMapping("/balance")
    public ResponseEntity<WalletBalanceResponse> getBalance(
            @RequestParam Long userId,
            @RequestParam Currency currency) {
        WalletBalanceResponse balance = walletService.getBalance(userId, currency);
        return ResponseEntity.ok(balance);
    }
}
