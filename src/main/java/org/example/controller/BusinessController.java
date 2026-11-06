package org.example.controller;

import org.example.dto.BorrowRequest;
import org.example.dto.ExtendLoanRequest;
import org.example.dto.PayFineRequest;
import org.example.entity.Loan;
import org.example.service.BusinessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/business")
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessService businessService;

    /** 1. Выдать книгу */
    @PostMapping("/borrow")
    public ResponseEntity<Loan> borrow(@Valid @RequestBody BorrowRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(businessService.borrowBook(request));
    }

    /** 2. Вернуть книгу */
    @PostMapping("/return/{loanId}")
    public Map<String, Object> returnBook(@PathVariable Long loanId) {
        return businessService.returnBook(loanId);
    }

    /** 3. Продлить срок */
    @PostMapping("/extend")
    public Loan extend(@Valid @RequestBody ExtendLoanRequest request) {
        return businessService.extendLoan(request);
    }

    /** 4. Оплатить штраф */
    @PostMapping("/pay-fine")
    public Map<String, Object> payFine(@Valid @RequestBody PayFineRequest request) {
        return businessService.payFine(request);
    }

    /** 5. Списать книгу как утерянную */
    @PostMapping("/write-off/{loanId}")
    public Map<String, Object> writeOff(@PathVariable Long loanId,
                                        @RequestParam BigDecimal replacementCost) {
        return businessService.writeOffLostBook(loanId, replacementCost);
    }

    /** Дополнительно: пометить просроченные */
    @PostMapping("/mark-overdue")
    public Map<String, Object> markOverdue() {
        int count = businessService.markOverdueLoans();
        return Map.of("markedOverdue", count);
    }
}
