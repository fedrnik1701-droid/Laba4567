package org.example.controller;

import org.example.dto.ReaderRequest;
import org.example.entity.Reader;
import org.example.service.ReaderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/readers")
@RequiredArgsConstructor
public class ReaderController {

    private final ReaderService readerService;

    @GetMapping
    public List<Reader> getAll() {
        return readerService.findAll();
    }

    @GetMapping("/{id}")
    public Reader getById(@PathVariable Long id) {
        return readerService.findById(id);
    }

    @PostMapping
    public ResponseEntity<Reader> create(@Valid @RequestBody ReaderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(readerService.create(request));
    }

    @PutMapping("/{id}")
    public Reader update(@PathVariable Long id, @Valid @RequestBody ReaderRequest request) {
        return readerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        readerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deactivate")
    public Reader deactivate(@PathVariable Long id) {
        return readerService.deactivate(id);
    }
}