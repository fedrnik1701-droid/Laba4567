package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.RegisterRequest;
import org.example.entity.Reader;
import org.example.exception.BusinessException;
import org.example.repository.ReaderRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RegistrationService {
    private final ReaderRepository readers;
    private final PasswordEncoder encoder;

    @Transactional
    public Reader register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (readers.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Account already exists");
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException("Password must not exceed 72 UTF-8 bytes");
        }
        return readers.save(Reader.builder()
                .fullName(request.fullName()).email(email).phone(request.phone())
                .password(encoder.encode(request.password()))
                .registrationDate(LocalDate.now()).build());
    }
}
