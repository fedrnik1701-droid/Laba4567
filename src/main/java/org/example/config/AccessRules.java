package org.example.config;

import lombok.RequiredArgsConstructor;
import org.example.repository.LoanRepository;
import org.example.repository.ReaderRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("access")
@RequiredArgsConstructor
public class AccessRules {
    private final ReaderRepository readers;
    private final LoanRepository loans;

    public boolean reader(Long id, Authentication auth) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_LIBRARIAN"))
                || readers.findById(id).map(r -> r.getEmail() != null
                        && r.getEmail().equalsIgnoreCase(auth.getName())).orElse(false);
    }

    public boolean loan(Long id, Authentication auth) {
        return loans.findById(id).map(l -> reader(l.getReader().getId(), auth)).orElse(false);
    }
}
