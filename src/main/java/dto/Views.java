package com.syfe.finance.dto;
import com.syfe.finance.entity.*; import java.math.*; import java.time.*;
public final class Views { private Views(){}
 public record TransactionView(Long id,BigDecimal amount,LocalDate date,String category,String description,String type){ public static TransactionView of(Transaction t){return new TransactionView(t.getId(),t.getAmount(),t.getDate(),t.getCategory().getName(),t.getDescription(),t.getCategory().getType().name());}}
 public record CategoryView(Long id,String name,String type,boolean isCustom){public static CategoryView of(Category c){return new CategoryView(c.getId(),c.getName(),c.getType().name(),c.isCustom());}}
 public record GoalView(Long id,String goalName,BigDecimal targetAmount,LocalDate targetDate,LocalDate startDate,BigDecimal currentProgress,BigDecimal progressPercentage,BigDecimal remainingAmount){}
}
