package org.example.service;

import org.example.dto.BorrowRequest;
import org.example.dto.ExtendLoanRequest;
import org.example.dto.PayFineRequest;
import org.example.entity.Book;
import org.example.entity.Loan;
import org.example.entity.Reader;
import org.example.exception.BusinessException;
import org.example.exception.ResourceNotFoundException;
import org.example.repository.BookRepository;
import org.example.repository.LoanRepository;
import org.example.repository.ReaderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Business operations for Library Management System.
 * These are not simple CRUD — they enforce domain rules and consistency.
 */
@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('LIBRARIAN')")
public class BusinessService {

    private static final int MAX_ACTIVE_LOANS_PER_READER = 5;
    private static final BigDecimal FINE_PER_DAY = new BigDecimal("10.00");

    private final BookRepository bookRepository;
    private final ReaderRepository readerRepository;
    private final LoanRepository loanRepository;

    /**
     * 1. Выдать книгу читателю (Borrow book).
     * Involves Book + Reader.
     * Checks: reader active, no outstanding fine above limit, available copies, max loans limit.
     */
    @Transactional
    public Loan borrowBook(BorrowRequest request) {
        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + request.getBookId()));
        Reader reader = readerRepository.findById(request.getReaderId())
                .orElseThrow(() -> new ResourceNotFoundException("Reader not found: " + request.getReaderId()));

        if (!Boolean.TRUE.equals(reader.getActive())) {
            throw new BusinessException("Reader account is deactivated");
        }
        if (reader.getFineBalance().compareTo(new BigDecimal("100.00")) > 0) {
            throw new BusinessException("Reader has unpaid fines exceeding 100. Pay fines first.");
        }
        if (book.getAvailableCopies() <= 0) {
            throw new BusinessException("No available copies of book: " + book.getTitle());
        }
        long activeLoans = loanRepository.countByReaderIdAndStatus(reader.getId(), Loan.LoanStatus.ACTIVE);
        if (activeLoans >= MAX_ACTIVE_LOANS_PER_READER) {
            throw new BusinessException("Reader already has maximum active loans (" + MAX_ACTIVE_LOANS_PER_READER + ")");
        }

        // Check if already has this book on active loan
        List<Loan> readerActive = loanRepository.findByReaderIdAndStatus(reader.getId(), Loan.LoanStatus.ACTIVE);
        boolean alreadyHas = readerActive.stream()
                .anyMatch(l -> l.getBook().getId().equals(book.getId()));
        if (alreadyHas) {
            throw new BusinessException("Reader already has an active loan for this book");
        }

        int days = request.getDays() != null && request.getDays() > 0 ? request.getDays() : 14;
        if (days > 30) {
            throw new BusinessException("Maximum loan period is 30 days");
        }

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);

        Loan loan = Loan.builder()
                .book(book)
                .reader(reader)
                .loanDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(days))
                .status(Loan.LoanStatus.ACTIVE)
                .build();
        return loanRepository.save(loan);
    }

    /**
     * 2. Вернуть книгу (Return book).
     * Involves Loan + Book (+ possibly Reader for fine calculation).
     * Calculates overdue fine if any.
     */
    @Transactional
    public Map<String, Object> returnBook(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found: " + loanId));

        if (loan.getStatus() == Loan.LoanStatus.RETURNED) {
            throw new BusinessException("Book already returned for this loan");
        }

        Book book = loan.getBook();
        Reader reader = loan.getReader();

        LocalDate today = LocalDate.now();
        loan.setReturnDate(today);
        loan.setStatus(Loan.LoanStatus.RETURNED);

        BigDecimal fine = BigDecimal.ZERO;
        if (today.isAfter(loan.getDueDate())) {
            long overdueDays = ChronoUnit.DAYS.between(loan.getDueDate(), today);
            fine = FINE_PER_DAY.multiply(BigDecimal.valueOf(overdueDays));
            reader.setFineBalance(reader.getFineBalance().add(fine));
            readerRepository.save(reader);
        }

        book.setAvailableCopies(book.getAvailableCopies() + 1);
        bookRepository.save(book);
        loanRepository.save(loan);

        Map<String, Object> result = new HashMap<>();
        result.put("loan", loan);
        result.put("fineCharged", fine);
        result.put("readerFineBalance", reader.getFineBalance());
        result.put("message", fine.compareTo(BigDecimal.ZERO) > 0
                ? "Book returned. Overdue fine charged: " + fine
                : "Book returned successfully on time");
        return result;
    }

    /**
     * 3. Продлить срок выдачи (Extend loan).
     * Involves Loan + Reader (checks fines).
     * Only for active loans, not already overdue beyond certain limit.
     */
    @Transactional
    @PreAuthorize("@access.loan(#request.loanId, authentication)")
    public Loan extendLoan(ExtendLoanRequest request) {
        Loan loan = loanRepository.findById(request.getLoanId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found: " + request.getLoanId()));

        if (loan.getStatus() != Loan.LoanStatus.ACTIVE) {
            throw new BusinessException("Only active loans can be extended");
        }

        Reader reader = loan.getReader();
        if (reader.getFineBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("Cannot extend loan while there are unpaid fines");
        }

        LocalDate newDue = loan.getDueDate().plusDays(request.getAdditionalDays());
        if (newDue.isAfter(LocalDate.now().plusDays(45))) {
            throw new BusinessException("Loan cannot be extended beyond 45 days from today");
        }

        // If currently overdue, mark and still allow limited extension only if not too late
        if (LocalDate.now().isAfter(loan.getDueDate())) {
            long overdue = ChronoUnit.DAYS.between(loan.getDueDate(), LocalDate.now());
            if (overdue > 7) {
                throw new BusinessException("Loan is overdue by more than 7 days. Return the book and pay fines.");
            }
        }

        loan.setDueDate(newDue);
        return loanRepository.save(loan);
    }

    /**
     * 4. Оплатить штраф (Pay fine).
     * Involves Reader.
     * Result depends on current fine balance.
     */
    @Transactional
    @PreAuthorize("@access.reader(#request.readerId, authentication)")
    public Map<String, Object> payFine(PayFineRequest request) {
        Reader reader = readerRepository.findById(request.getReaderId())
                .orElseThrow(() -> new ResourceNotFoundException("Reader not found: " + request.getReaderId()));

        if (reader.getFineBalance().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Reader has no outstanding fines");
        }
        if (request.getAmount().compareTo(reader.getFineBalance()) > 0) {
            throw new BusinessException("Payment amount exceeds fine balance. Current balance: " + reader.getFineBalance());
        }

        BigDecimal newBalance = reader.getFineBalance().subtract(request.getAmount());
        reader.setFineBalance(newBalance);
        readerRepository.save(reader);

        Map<String, Object> result = new HashMap<>();
        result.put("readerId", reader.getId());
        result.put("paid", request.getAmount());
        result.put("remainingFine", newBalance);
        result.put("message", newBalance.compareTo(BigDecimal.ZERO) == 0
                ? "All fines paid. Account clear."
                : "Partial payment accepted. Remaining: " + newBalance);
        return result;
    }

    /**
     * 5. Списать книгу (Write-off / mark lost book from active loan).
     * Involves Loan + Book + Reader.
     * Charges replacement cost as fine, removes copy from total.
     */
    @Transactional
    public Map<String, Object> writeOffLostBook(Long loanId, BigDecimal replacementCost) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found: " + loanId));

        if (loan.getStatus() != Loan.LoanStatus.ACTIVE) {
            throw new BusinessException("Only active loans can be written off as lost");
        }
        if (replacementCost == null || replacementCost.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Replacement cost must be positive");
        }

        Book book = loan.getBook();
        Reader reader = loan.getReader();

        // Charge replacement cost as fine
        reader.setFineBalance(reader.getFineBalance().add(replacementCost));
        readerRepository.save(reader);

        // Reduce total and available (available already decreased on borrow)
        book.setTotalCopies(book.getTotalCopies() - 1);
        // available stays the same because it was already decremented
        if (book.getTotalCopies() < 0) {
            book.setTotalCopies(0);
        }
        bookRepository.save(book);

        loan.setStatus(Loan.LoanStatus.RETURNED); // close the loan
        loan.setReturnDate(LocalDate.now());
        loanRepository.save(loan);

        Map<String, Object> result = new HashMap<>();
        result.put("loanId", loan.getId());
        result.put("bookTitle", book.getTitle());
        result.put("replacementCostCharged", replacementCost);
        result.put("readerFineBalance", reader.getFineBalance());
        result.put("remainingCopies", book.getTotalCopies());
        result.put("message", "Book written off as lost. Replacement cost added to reader's fine.");
        return result;
    }

    /**
     * Helper: mark overdue loans (can be called periodically).
     */
    @Transactional
    public int markOverdueLoans() {
        List<Loan> active = loanRepository.findByStatus(Loan.LoanStatus.ACTIVE);
        int count = 0;
        LocalDate today = LocalDate.now();
        for (Loan loan : active) {
            if (today.isAfter(loan.getDueDate())) {
                loan.setStatus(Loan.LoanStatus.OVERDUE);
                loanRepository.save(loan);
                count++;
            }
        }
        return count;
    }
}
