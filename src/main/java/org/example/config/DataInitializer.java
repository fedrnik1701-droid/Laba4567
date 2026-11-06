package org.example.config;

import org.example.entity.Book;
import org.example.entity.Category;
import org.example.entity.Reader;
import org.example.repository.BookRepository;
import org.example.repository.CategoryRepository;
import org.example.repository.ReaderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;
    private final ReaderRepository readerRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (categoryRepository.count() > 0) {
            return; // already initialized
        }

        Category fiction = categoryRepository.save(Category.builder()
                .name("Художественная литература")
                .description("Романы, рассказы, поэзия")
                .build());
        Category science = categoryRepository.save(Category.builder()
                .name("Научная литература")
                .description("Научно-популярные и учебные издания")
                .build());
        Category programming = categoryRepository.save(Category.builder()
                .name("Программирование")
                .description("Книги по IT и разработке ПО")
                .build());

        bookRepository.save(Book.builder()
                .title("Война и мир")
                .author("Лев Толстой")
                .isbn("978-5-389-01234-5")
                .totalCopies(5)
                .availableCopies(5)
                .categories(Set.of(fiction))
                .build());

        bookRepository.save(Book.builder()
                .title("Clean Code")
                .author("Robert C. Martin")
                .isbn("978-0-13-235088-4")
                .totalCopies(3)
                .availableCopies(3)
                .categories(Set.of(programming, science))
                .build());

        bookRepository.save(Book.builder()
                .title("Мастер и Маргарита")
                .author("Михаил Булгаков")
                .isbn("978-5-17-012345-6")
                .totalCopies(4)
                .availableCopies(4)
                .categories(Set.of(fiction))
                .build());

        bookRepository.save(Book.builder()
                .title("Effective Java")
                .author("Joshua Bloch")
                .isbn("978-0-13-468599-1")
                .totalCopies(2)
                .availableCopies(2)
                .categories(Set.of(programming))
                .build());

        readerRepository.save(Reader.builder()
                .fullName("Иван Иванов")
                .email("ivan@example.com")
                .password(passwordEncoder.encode("Reader123!"))
                .phone("+7-900-111-22-33")
                .registrationDate(LocalDate.now().minusMonths(6))
                .build());

        readerRepository.save(Reader.builder()
                .fullName("Мария Петрова")
                .email("maria@example.com")
                .password(passwordEncoder.encode("Reader123!"))
                .phone("+7-900-444-55-66")
                .registrationDate(LocalDate.now().minusMonths(2))
                .build());

        readerRepository.save(Reader.builder()
                .fullName("Алексей Сидоров")
                .email("alex@example.com")
                .password(passwordEncoder.encode("Reader123!"))
                .active(false)
                .phone("+7-900-777-88-99")
                .registrationDate(LocalDate.now().minusDays(10))
                .build());

        readerRepository.save(Reader.builder()
                .fullName("Библиотекарь").email("librarian@example.com")
                .password(passwordEncoder.encode("Librarian123!"))
                .role(Reader.Role.LIBRARIAN)
                .registrationDate(LocalDate.now()).build());
    }
}
