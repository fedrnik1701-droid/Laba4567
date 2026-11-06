package org.example.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Data
public class BookRequest {
    @NotBlank
    private String title;
    @NotBlank
    private String author;
    private String isbn;
    @NotNull
    @Min(1)
    private Integer totalCopies;
    private Set<Long> categoryIds;
}