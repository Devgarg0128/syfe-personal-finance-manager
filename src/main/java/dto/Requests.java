package com.syfe.finance.dto;
import jakarta.validation.constraints.*; import java.math.BigDecimal; import java.time.LocalDate;
public final class Requests { private Requests(){}
 public record Register(@NotBlank @Email String username,@NotBlank @Size(min=8,max=72) String password,@NotBlank @Size(max=120) String fullName,@NotBlank @Pattern(regexp="^[+0-9() -]{7,30}$") String phoneNumber){}
 public record Login(@NotBlank @Email String username,@NotBlank String password){}
 public record TransactionCreate(@NotNull @DecimalMin(value="0.01") @Digits(integer=17,fraction=2) BigDecimal amount,@NotNull LocalDate date,@NotBlank String category,@Size(max=500) String description){}
 public record TransactionUpdate(@DecimalMin(value="0.01") @Digits(integer=17,fraction=2) BigDecimal amount, @Size(max=500) String description, String category){}
 public record CategoryCreate(@NotBlank @Size(max=80) String name,@NotNull com.syfe.finance.entity.CategoryType type){}
 public record GoalCreate(@NotBlank @Size(max=120) String goalName,@NotNull @DecimalMin(value="0.01") @Digits(integer=17,fraction=2) BigDecimal targetAmount,@NotNull LocalDate targetDate,LocalDate startDate){}
 public record GoalUpdate(@DecimalMin(value="0.01") @Digits(integer=17,fraction=2) BigDecimal targetAmount,LocalDate targetDate){}
}
