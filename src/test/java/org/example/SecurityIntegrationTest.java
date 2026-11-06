package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.entity.Reader;
import org.example.repository.BookRepository;
import org.example.repository.LoanRepository;
import org.example.repository.ReaderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ReaderRepository readers;
    @Autowired BookRepository books;
    @Autowired LoanRepository loans;
    @Autowired PasswordEncoder encoder;

    private Reader ivan() {
        return readers.findByEmailIgnoreCase("ivan@example.com").orElseThrow();
    }

    private Reader maria() {
        return readers.findByEmailIgnoreCase("maria@example.com").orElseThrow();
    }

    private long borrow() throws Exception {
        String result = mvc.perform(post("/api/business/borrow")
                        .with(httpBasic("librarian@example.com", "Librarian123!")).with(csrf())
                        .contentType("application/json")
                        .content("{\"bookId\":1,\"readerId\":" + ivan().getId() + "}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(result).get("id").asLong();
    }

    @Test
    void authenticationAndDisabledAccount() throws Exception {
        mvc.perform(get("/api/books")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/books").with(httpBasic("ivan@example.com", "wrong")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/books").with(httpBasic("alex@example.com", "Reader123!")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/books").with(httpBasic("ivan@example.com", "Reader123!")))
                .andExpect(status().isOk());
        mvc.perform(post("/api/readers/{id}/deactivate", ivan().getId()).with(csrf())
                        .with(httpBasic("librarian@example.com", "Librarian123!")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/books").with(httpBasic("ivan@example.com", "Reader123!")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registrationEncodesPasswordAndIgnoresPrivileges() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
                        .content("""
                                {"fullName":"New reader","email":"NEW@example.com",
                                 "password":"Strong123!","role":"LIBRARIAN","active":false}
                                """))
                .andExpect(status().isCreated()).andExpect(jsonPath("password").doesNotExist())
                .andExpect(jsonPath("role").value("READER")).andExpect(jsonPath("active").value(true));
        Reader registered = readers.findByEmailIgnoreCase("new@example.com").orElseThrow();
        assertTrue(registered.getPassword().startsWith("$2a$"));
        assertTrue(encoder.matches("Strong123!", registered.getPassword()));
        mvc.perform(get("/api/readers").with(httpBasic("new@example.com", "Strong123!")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
                        .content("{\"fullName\":\"Duplicate\",\"email\":\"new@example.com\",\"password\":\"Strong123!\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void weakAndOverlongPasswordsAreRejected() throws Exception {
        for (String password : new String[]{"short", "alllowercase123", "UPPERCASE123", "NoDigitsHere",
                "Ab1" + "я".repeat(35)}) {
            mvc.perform(post("/api/auth/register").with(csrf()).contentType("application/json")
                            .content(json.writeValueAsString(java.util.Map.of("fullName", "Test",
                                    "email", "weak@example.com", "password", password))))
                    .andExpect(status().isBadRequest());
        }
        assertFalse(readers.existsByEmailIgnoreCase("weak@example.com"));
    }

    @Test
    void realCsrfTokenRequiresMatchingSession() throws Exception {
        var result = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        var token = json.readTree(result.getResponse().getContentAsString());
        var session = (MockHttpSession) result.getRequest().getSession(false);
        String body = "{\"fullName\":\"CSRF test\",\"email\":\"csrf@example.com\",\"password\":\"Strong123!\"}";
        mvc.perform(post("/api/auth/register").session(session).contentType("application/json").content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/register").session(session).header("X-CSRF-TOKEN", "wrong")
                        .contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/register").header(token.get("headerName").asText(), token.get("token").asText())
                        .contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/register").session(session)
                        .header(token.get("headerName").asText(), token.get("token").asText())
                        .contentType("application/json").content(body)).andExpect(status().isCreated());
        mvc.perform(post("/api/categories").with(httpBasic("librarian@example.com", "Librarian123!"))
                        .contentType("application/json").content("{\"name\":\"No CSRF\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void readerCanOnlyReadOwnProfileAndLoans() throws Exception {
        long loan = borrow();
        mvc.perform(get("/api/readers/{id}", ivan().getId()).with(httpBasic("ivan@example.com", "Reader123!")))
                .andExpect(status().isOk()).andExpect(jsonPath("password").doesNotExist());
        mvc.perform(get("/api/readers/{id}", maria().getId()).with(httpBasic("ivan@example.com", "Reader123!")))
                .andExpect(status().isForbidden());
        for (String path : new String[]{"/api/readers", "/api/loans", "/api/loans/active", "/api/loans/book/1",
                "/api/loans/reader/" + ivan().getId(), "/api/loans/" + loan}) {
            mvc.perform(get(path).with(httpBasic("maria@example.com", "Reader123!")))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/loans/reader/{id}", ivan().getId()).with(httpBasic("ivan@example.com", "Reader123!")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].reader.password").doesNotExist());
        mvc.perform(get("/api/loans/{id}", loan).with(httpBasic("ivan@example.com", "Reader123!")))
                .andExpect(status().isOk());
    }

    @Test
    void readerCannotManageCatalogOrIssueBooks() throws Exception {
        mvc.perform(post("/api/books").with(httpBasic("ivan@example.com", "Reader123!")).with(csrf())
                        .contentType("application/json").content("{\"title\":\"Test\",\"author\":\"Test\",\"totalCopies\":1}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/categories/1").with(httpBasic("ivan@example.com", "Reader123!")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/business/borrow").with(httpBasic("ivan@example.com", "Reader123!")).with(csrf())
                        .contentType("application/json").content("{\"bookId\":1,\"readerId\":" + ivan().getId() + "}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownershipAndBusinessRulesBothApply() throws Exception {
        long loan = borrow();
        String extend = "{\"loanId\":" + loan + ",\"additionalDays\":3}";
        mvc.perform(post("/api/business/extend").with(httpBasic("maria@example.com", "Reader123!")).with(csrf())
                        .contentType("application/json").content(extend)).andExpect(status().isForbidden());
        mvc.perform(post("/api/business/extend").with(httpBasic("ivan@example.com", "Reader123!")).with(csrf())
                        .contentType("application/json").content(extend)).andExpect(status().isOk());
        Reader reader = ivan();
        reader.setFineBalance(new BigDecimal("20.00"));
        readers.saveAndFlush(reader);
        mvc.perform(post("/api/business/extend").with(httpBasic("ivan@example.com", "Reader123!")).with(csrf())
                        .contentType("application/json").content(extend)).andExpect(status().isBadRequest());
        String payment = "{\"readerId\":" + reader.getId() + ",\"amount\":20}";
        mvc.perform(post("/api/business/pay-fine").with(httpBasic("maria@example.com", "Reader123!")).with(csrf())
                        .contentType("application/json").content(payment)).andExpect(status().isForbidden());
        mvc.perform(post("/api/business/pay-fine").with(httpBasic("ivan@example.com", "Reader123!")).with(csrf())
                        .contentType("application/json").content(payment)).andExpect(status().isOk());
        assertEquals(0, reader.getFineBalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    void librarianOperationsKeepOriginalConstraints() throws Exception {
        int available = books.findById(1L).orElseThrow().getAvailableCopies();
        long loan = borrow();
        assertEquals(available - 1, books.findById(1L).orElseThrow().getAvailableCopies());
        mvc.perform(delete("/api/books/1").with(httpBasic("librarian@example.com", "Librarian123!")).with(csrf()))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/business/return/{id}", loan).with(httpBasic("librarian@example.com", "Librarian123!")).with(csrf()))
                .andExpect(status().isOk());
        assertEquals(available, books.findById(1L).orElseThrow().getAvailableCopies());
        mvc.perform(post("/api/business/return/{id}", loan).with(httpBasic("librarian@example.com", "Librarian123!")).with(csrf()))
                .andExpect(status().isBadRequest());
        long lost = borrow();
        mvc.perform(post("/api/business/write-off/{id}", lost).param("replacementCost", "50")
                        .with(httpBasic("librarian@example.com", "Librarian123!")).with(csrf()))
                .andExpect(status().isOk());
        assertEquals(0, ivan().getFineBalance().compareTo(new BigDecimal("50")));
        assertEquals(available - 1, books.findById(1L).orElseThrow().getTotalCopies());
    }
}
