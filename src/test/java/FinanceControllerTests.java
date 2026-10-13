package com.syfe.finance.controller;

import com.syfe.finance.dto.Requests;
import com.syfe.finance.dto.Views;
import com.syfe.finance.entity.CategoryType;
import com.syfe.finance.service.FinanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinanceControllerTests {

    @Mock
    FinanceService service;

    FinanceController controller;
    Principal principal;

    @BeforeEach
    void setUp() {
        controller = new FinanceController(service);
        principal = () -> "devtest@example.com";
    }

    @Test
    void listCategoriesReturnsCategories() {
        when(service.listCategories(principal.getName())).thenReturn(List.of());

        assertTrue(controller.categories(principal).containsKey("categories"));
    }

    @Test
    void createCategoryReturnsCreated() {
        Requests.CategoryCreate request =
                new Requests.CategoryCreate("Travel", CategoryType.EXPENSE);

        when(service.createCategory(principal.getName(), request)).thenReturn(null);

        assertEquals(HttpStatus.CREATED,
                controller.createCategory(principal, request).getStatusCode());
    }

    @Test
    void deleteCategoryDelegatesToService() {
        when(service.deleteCategory(principal.getName(), "Travel"))
                .thenReturn(Map.of("message", "Category deleted"));

        assertEquals("Category deleted",
                controller.deleteCategory(principal, "Travel").get("message"));
    }

    @Test
    void createTransactionReturnsCreated() {
        Requests.TransactionCreate request = new Requests.TransactionCreate(
                new BigDecimal("250.50"), LocalDate.now(), "Entertainment", "Test");

        when(service.createTransaction(principal.getName(), request)).thenReturn(null);

        assertEquals(HttpStatus.CREATED,
                controller.createTransaction(principal, request).getStatusCode());
    }

    @Test
    void listTransactionsPassesFiltersToService() {
        LocalDate today = LocalDate.now();
        when(service.listTransactions(
                principal.getName(), today, today, 2L, "EXPENSE"))
                .thenReturn(List.of());

        Map<String, Object> result = controller.listTransactions(
                principal, today, today, 2L, "EXPENSE");

        assertTrue(result.containsKey("transactions"));
        verify(service).listTransactions(
                principal.getName(), today, today, 2L, "EXPENSE");
    }

    @Test
    void updateTransactionDelegatesToService() {
        Requests.TransactionUpdate request =
                new Requests.TransactionUpdate(
                        new BigDecimal("300.00"), "Updated", null);

        when(service.updateTransaction(principal.getName(), 1L, request))
                .thenReturn(null);

        assertNull(controller.updateTransaction(principal, 1L, request));
    }

    @Test
    void deleteTransactionDelegatesToService() {
        when(service.deleteTransaction(principal.getName(), 1L))
                .thenReturn(Map.of("message", "Transaction deleted"));

        assertEquals("Transaction deleted",
                controller.deleteTransaction(principal, 1L).get("message"));
    }

    @Test
    void createGoalReturnsCreated() {
        Requests.GoalCreate request = new Requests.GoalCreate(
                "Emergency Fund",
                new BigDecimal("50000"),
                LocalDate.now().plusMonths(6),
                LocalDate.now());

        when(service.createGoal(principal.getName(), request)).thenReturn(null);

        assertEquals(HttpStatus.CREATED,
                controller.createGoal(principal, request).getStatusCode());
    }

    @Test
    void listGoalsReturnsGoals() {
        when(service.listGoals(principal.getName())).thenReturn(List.of());

        assertTrue(controller.goals(principal).containsKey("goals"));
    }

    @Test
    void getGoalDelegatesToService() {
        when(service.getGoal(principal.getName(), 1L)).thenReturn(null);

        assertNull(controller.getGoal(principal, 1L));
    }

    @Test
    void updateGoalDelegatesToService() {
        Requests.GoalUpdate request = new Requests.GoalUpdate(
                new BigDecimal("60000"), LocalDate.now().plusMonths(8));

        when(service.updateGoal(principal.getName(), 1L, request))
                .thenReturn(null);

        assertNull(controller.updateGoal(principal, 1L, request));
    }

    @Test
    void deleteGoalDelegatesToService() {
        when(service.deleteGoal(principal.getName(), 1L))
                .thenReturn(Map.of("message", "Goal deleted"));

        assertEquals("Goal deleted",
                controller.deleteGoal(principal, 1L).get("message"));
    }

    @Test
    void monthlyReportDelegatesToService() {
        when(service.monthlyReport(principal.getName(), 2026, 10))
                .thenReturn(Map.of("month", 10));

        assertEquals(10,
                controller.monthly(principal, 2026, 10).get("month"));
    }

    @Test
    void yearlyReportDelegatesToService() {
        when(service.yearlyReport(principal.getName(), 2026))
                .thenReturn(Map.of("year", 2026));

        assertEquals(2026,
                controller.yearly(principal, 2026).get("year"));
    }
    
    @Test
    void listCategoriesPassesPrincipalToService() {
        when(service.listCategories(principal.getName()))
                .thenReturn(List.of());

        controller.categories(principal);

        verify(service).listCategories("devtest@example.com");
    }

    @Test
    void createCategoryPassesCorrectRequestToService() {
        Requests.CategoryCreate request =
                new Requests.CategoryCreate("Travel", CategoryType.EXPENSE);

        when(service.createCategory(principal.getName(), request))
                .thenReturn(null);

        controller.createCategory(principal, request);

        verify(service).createCategory("devtest@example.com", request);
    }

    @Test
    void createTransactionPassesCorrectRequestToService() {
        Requests.TransactionCreate request =
                new Requests.TransactionCreate(
                        new BigDecimal("250.50"),
                        LocalDate.now(),
                        "Entertainment",
                        "Test");

        when(service.createTransaction(principal.getName(), request))
                .thenReturn(null);

        controller.createTransaction(principal, request);

        verify(service).createTransaction("devtest@example.com", request);
    }

    @Test
    void createGoalPassesCorrectRequestToService() {
        Requests.GoalCreate request = new Requests.GoalCreate(
                "Emergency Fund",
                new BigDecimal("50000"),
                LocalDate.now().plusMonths(6),
                LocalDate.now());

        when(service.createGoal(principal.getName(), request))
                .thenReturn(null);

        controller.createGoal(principal, request);

        verify(service).createGoal("devtest@example.com", request);
    }

    @Test
    void monthlyReportUsesRequestedYearAndMonth() {
        when(service.monthlyReport(principal.getName(), 2026, 10))
                .thenReturn(Map.of("month", 10));

        controller.monthly(principal, 2026, 10);

        verify(service).monthlyReport("devtest@example.com", 2026, 10);
    }

    @Test
    void yearlyReportUsesRequestedYear() {
        when(service.yearlyReport(principal.getName(), 2026))
                .thenReturn(Map.of("year", 2026));

        controller.yearly(principal, 2026);

        verify(service).yearlyReport("devtest@example.com", 2026);
    }

}