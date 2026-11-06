package org.example.repository;

import org.example.entity.Reader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReaderRepository extends JpaRepository<Reader, Long> {
    Optional<Reader> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}
