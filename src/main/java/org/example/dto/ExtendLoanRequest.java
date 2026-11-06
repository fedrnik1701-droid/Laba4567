package org.example.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExtendLoanRequest {
    @NotNull
    private Long loanId;
    @NotNull
    @Min(1)
    private Integer additionalDays;
}