package com.syfe.finance.controller;
import com.syfe.finance.dto.*; import com.syfe.finance.service.FinanceService; import jakarta.validation.Valid; import org.springframework.http.*; import org.springframework.web.bind.annotation.*; import java.time.LocalDate; import java.util.*;
@RestController public class FinanceController {
 private final FinanceService s; public FinanceController(FinanceService s){this.s=s;}
 @GetMapping("/api/categories") Map<String,Object> categories(java.security.Principal p){return Map.of("categories",s.listCategories(p.getName()));}
 @PostMapping("/api/categories") ResponseEntity<Views.CategoryView> createCategory(java.security.Principal p,@Valid @RequestBody Requests.CategoryCreate r){return ResponseEntity.status(201).body(s.createCategory(p.getName(),r));}
 @DeleteMapping("/api/categories/{name}") Map<String,String> deleteCategory(java.security.Principal p,@PathVariable String name){return s.deleteCategory(p.getName(),name);}
 @PostMapping("/api/transactions") ResponseEntity<Views.TransactionView> createTransaction(java.security.Principal p,@Valid @RequestBody Requests.TransactionCreate r){return ResponseEntity.status(201).body(s.createTransaction(p.getName(),r));}
 @GetMapping("/api/transactions") Map<String,Object> listTransactions(java.security.Principal p,@RequestParam(required=false) LocalDate startDate,@RequestParam(required=false) LocalDate endDate,@RequestParam(required=false) Long categoryId,@RequestParam(required=false) String type){return Map.of("transactions",s.listTransactions(p.getName(),startDate,endDate,categoryId,type));}
 @PutMapping("/api/transactions/{id}") Views.TransactionView updateTransaction(java.security.Principal p,@PathVariable Long id,@Valid @RequestBody Requests.TransactionUpdate r){return s.updateTransaction(p.getName(),id,r);}
 @DeleteMapping("/api/transactions/{id}") Map<String,String> deleteTransaction(java.security.Principal p,@PathVariable Long id){return s.deleteTransaction(p.getName(),id);}
 @PostMapping("/api/goals") ResponseEntity<Views.GoalView> createGoal(java.security.Principal p,@Valid @RequestBody Requests.GoalCreate r){return ResponseEntity.status(201).body(s.createGoal(p.getName(),r));}
 @GetMapping("/api/goals") Map<String,Object> goals(java.security.Principal p){return Map.of("goals",s.listGoals(p.getName()));}
 @GetMapping("/api/goals/{id}") Views.GoalView getGoal(java.security.Principal p,@PathVariable Long id){return s.getGoal(p.getName(),id);}
 @PutMapping("/api/goals/{id}") Views.GoalView updateGoal(java.security.Principal p,@PathVariable Long id,@Valid @RequestBody Requests.GoalUpdate r){return s.updateGoal(p.getName(),id,r);}
 @DeleteMapping("/api/goals/{id}") Map<String,String> deleteGoal(java.security.Principal p,@PathVariable Long id){return s.deleteGoal(p.getName(),id);}
 @GetMapping("/api/reports/monthly/{year}/{month}") Map<String,Object> monthly(java.security.Principal p,@PathVariable int year,@PathVariable int month){return s.monthlyReport(p.getName(),year,month);}
 @GetMapping("/api/reports/yearly/{year}") Map<String,Object> yearly(java.security.Principal p,@PathVariable int year){return s.yearlyReport(p.getName(),year);}
}
