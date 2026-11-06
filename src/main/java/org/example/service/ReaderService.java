package org.example.service;

import org.example.dto.ReaderRequest;
import org.example.entity.Reader;
import org.example.exception.BusinessException;
import org.example.exception.ResourceNotFoundException;
import org.example.repository.ReaderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('LIBRARIAN')")
public class ReaderService {

    private final ReaderRepository readerRepository;

    public List<Reader> findAll() {
        return readerRepository.findAll();
    }

    @PreAuthorize("@access.reader(#id, authentication)")
    public Reader findById(Long id) {
        return readerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reader not found with id: " + id));
    }

    @Transactional
    public Reader create(ReaderRequest request) {
        if (request.getEmail() != null) request.setEmail(request.getEmail().toLowerCase(Locale.ROOT));
        if (request.getEmail() != null && readerRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BusinessException("Reader with email already exists: " + request.getEmail());
        }
        Reader reader = Reader.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .registrationDate(LocalDate.now())
                .build();
        return readerRepository.save(reader);
    }

    @Transactional
    public Reader update(Long id, ReaderRequest request) {
        if (request.getEmail() != null) request.setEmail(request.getEmail().toLowerCase(Locale.ROOT));
        Reader reader = findById(id);
        if (request.getEmail() != null && !request.getEmail().equals(reader.getEmail())
                && readerRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new BusinessException("Reader with email already exists: " + request.getEmail());
        }
        reader.setFullName(request.getFullName());
        reader.setEmail(request.getEmail());
        reader.setPhone(request.getPhone());
        return readerRepository.save(reader);
    }

    @Transactional
    public void delete(Long id) {
        Reader reader = findById(id);
        readerRepository.delete(reader);
    }

    @Transactional
    public Reader deactivate(Long id) {
        Reader reader = findById(id);
        reader.setActive(false);
        return readerRepository.save(reader);
    }
}
