package org.example.service;

import org.example.dto.BookRequest;
import org.example.entity.Book;
import org.example.entity.Category;
import org.example.exception.BusinessException;
import org.example.exception.ResourceNotFoundException;
import org.example.repository.BookRepository;
import org.example.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;

    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    public Book findById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
    }

    @Transactional
    public Book create(BookRequest request) {
        if (request.getIsbn() != null && bookRepository.existsByIsbn(request.getIsbn())) {
            throw new BusinessException("Book with ISBN already exists: " + request.getIsbn());
        }
        Set<Category> categories = resolveCategories(request.getCategoryIds());
        Book book = Book.builder()
                .title(request.getTitle())
                .author(request.getAuthor())
                .isbn(request.getIsbn())
                .totalCopies(request.getTotalCopies())
                .availableCopies(request.getTotalCopies())
                .categories(categories)
                .build();
        return bookRepository.save(book);
    }

    @Transactional
    public Book update(Long id, BookRequest request) {
        Book book = findById(id);
        if (request.getIsbn() != null && !request.getIsbn().equals(book.getIsbn())
                && bookRepository.existsByIsbn(request.getIsbn())) {
            throw new BusinessException("Book with ISBN already exists: " + request.getIsbn());
        }
        int diff = request.getTotalCopies() - book.getTotalCopies();
        if (book.getAvailableCopies() + diff < 0) {
            throw new BusinessException("Cannot reduce total copies below currently borrowed ones");
        }
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setIsbn(request.getIsbn());
        book.setTotalCopies(request.getTotalCopies());
        book.setAvailableCopies(book.getAvailableCopies() + diff);
        if (request.getCategoryIds() != null) {
            book.setCategories(resolveCategories(request.getCategoryIds()));
        }
        return bookRepository.save(book);
    }

    @Transactional
    public void delete(Long id) {
        Book book = findById(id);
        if (book.getAvailableCopies() < book.getTotalCopies()) {
            throw new BusinessException("Cannot delete book that has active loans");
        }
        bookRepository.delete(book);
    }

    private Set<Category> resolveCategories(Set<Long> categoryIds) {
        Set<Category> categories = new HashSet<>();
        if (categoryIds != null) {
            for (Long catId : categoryIds) {
                Category cat = categoryRepository.findById(catId)
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + catId));
                categories.add(cat);
            }
        }
        return categories;
    }
}