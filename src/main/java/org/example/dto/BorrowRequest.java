package org.example.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BorrowRequest {
    @NotNull
    private Long bookId;
    @NotNull
    private Long readerId;
    private Integer days = 14; // default loan period
}