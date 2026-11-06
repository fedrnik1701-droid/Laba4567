package org.example.controller;

import org.example.entity.Loan;
import org.example.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @GetMapping
    public List<Loan> getAll() {
        return loanService.findAll();
    }

    @GetMapping("/{id}")
    public Loan getById(@PathVariable Long id) {
        return loanService.findById(id);
    }

    @GetMapping("/reader/{readerId}")
    public List<Loan> getByReader(@PathVariable Long readerId) {
        return loanService.findByReader(readerId);
    }

    @GetMapping("/book/{bookId}")
    public List<Loan> getByBook(@PathVariable Long bookId) {
        return loanService.findByBook(bookId);
    }

    @GetMapping("/active")
    public List<Loan> getActive() {
        return loanService.findActive();
    }
}