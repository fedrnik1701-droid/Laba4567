package org.example.repository;

import org.example.entity.Loan;
import org.example.entity.Loan.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByReaderId(Long readerId);
    List<Loan> findByBookId(Long bookId);
    List<Loan> findByStatus(LoanStatus status);
    List<Loan> findByReaderIdAndStatus(Long readerId, LoanStatus status);
    long countByReaderIdAndStatus(Long readerId, LoanStatus status);
}