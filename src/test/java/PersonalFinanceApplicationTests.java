
package com.syfe.finance;

import com.syfe.finance.dto.Requests;
import com.syfe.finance.entity.*;
import com.syfe.finance.exception.ApiException;
import com.syfe.finance.repository.*;
import com.syfe.finance.service.FinanceService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonalFinanceApplicationTests {

    @Mock UserRepository users;
    @Mock CategoryRepository categories;
    @Mock TransactionRepository transactions;
    @Mock GoalRepository goals;
    @Mock PasswordEncoder encoder;

    FinanceService service;

    @BeforeEach
    void setUp() {
        service = new FinanceService(
                users, categories, transactions, goals, encoder
        );
    }

    // 1. Registration normalizes email and hashes password
    @Test
    void registerNormalizesEmailAndHashesPassword() throws Exception {
        when(users.existsByUsernameIgnoreCase("user@example.com"))
                .thenReturn(false);
        when(encoder.encode("password123")).thenReturn("hashed");

        when(users.save(any(AppUser.class))).thenAnswer(inv -> {
            AppUser u = inv.getArgument(0);
            setId(u, 7L);
            return u;
        });

        var result = service.register(new Requests.Register(
                " User@Example.com ",
                "password123",
                " Test User ",
                "+919876543210"
        ));

        assertEquals("User registered successfully", result.get("message"));
        assertEquals(7L, ((Number) result.get("userId")).longValue());

        verify(users).save(argThat(u ->
                u.getUsername().equals("user@example.com")
                        && u.getPasswordHash().equals("hashed")
                        && u.getFullName().equals("Test User")
        ));
    }

    // 2. Duplicate registration is rejected
    @Test
    void registerRejectsDuplicateUsername() {
        when(users.existsByUsernameIgnoreCase("user@example.com"))
                .thenReturn(true);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.register(new Requests.Register(
                        "user@example.com", "password123",
                        "Test", "12345678"
                )));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(users, never()).save(any());
    }

    // 3. Missing user returns unauthorized
    @Test
    void missingUserReturnsUnauthorized() {
        when(users.findByUsernameIgnoreCase("nobody@example.com"))
                .thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.user("nobody@example.com"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    // 4. Categories are sorted and mapped
    @Test
    void listCategoriesSortsAndMapsCategories() throws Exception {
        AppUser owner = user(1L);

        when(users.findByUsernameIgnoreCase("u@example.com"))
                .thenReturn(Optional.of(owner));

        when(categories.findByOwner_IdOrOwnerIsNull(1L))
                .thenReturn(List.of(
                        new Category("Zebra", CategoryType.EXPENSE, owner, true),
                        new Category("Salary", CategoryType.INCOME, null, false)
                ));

        var result = service.listCategories("u@example.com");

        assertEquals("Salary", result.get(0).name());
        assertEquals("Zebra", result.get(1).name());
        assertFalse(result.get(0).isCustom());
    }

    // 5. Duplicate category name is rejected
    @Test
    void createCategoryRejectsNameUsedByOwnerOrDefault() throws Exception {
        AppUser owner = user(1L);

        when(users.findByUsernameIgnoreCase("u@example.com"))
                .thenReturn(Optional.of(owner));
        when(categories.existsByNameIgnoreCaseAndOwner_Id("Food", 1L))
                .thenReturn(false);
        when(categories.existsByNameIgnoreCaseAndOwnerIsNull("Food"))
                .thenReturn(true);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.createCategory(
                        "u@example.com",
                        new Requests.CategoryCreate("Food", CategoryType.EXPENSE)
                ));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    // 6. Custom category is saved
    @Test
    void createCategorySavesCustomCategory() throws Exception {
        AppUser owner = user(1L);

        when(users.findByUsernameIgnoreCase("u@example.com"))
                .thenReturn(Optional.of(owner));
        when(categories.existsByNameIgnoreCaseAndOwner_Id("Travel", 1L))
                .thenReturn(false);
        when(categories.existsByNameIgnoreCaseAndOwnerIsNull("Travel"))
                .thenReturn(false);
        when(categories.save(any(Category.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var view = service.createCategory(
                "u@example.com",
                new Requests.CategoryCreate(" Travel ", CategoryType.EXPENSE)
        );

        assertEquals("Travel", view.name());
        assertTrue(view.isCustom());
    }

    // 7. Default categories cannot be deleted
    @Test
    void defaultCategoryCannotBeDeleted() throws Exception {
        when(users.findByUsernameIgnoreCase("u@example.com"))
                .thenReturn(Optional.of(user(1L)));
        when(categories.findByNameIgnoreCaseAndOwnerIsNull("Food"))
                .thenReturn(Optional.of(
                        new Category("Food", CategoryType.EXPENSE, null, false)
                ));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.deleteCategory("u@example.com", "Food"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    // 8. Transaction date cannot be in the future
    @Test
    void transactionDateCannotBeInFuture() {
        var request = new Requests.TransactionCreate(
                new BigDecimal("5.00"),
                LocalDate.now().plusDays(1),
                "Food",
                "test"
        );

        ApiException ex = assertThrows(ApiException.class,
                () -> service.createTransaction("u@example.com", request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    // 9. Transaction uses the owner and default category
    @Test
    void createTransactionUsesOwnerAndCategory() throws Exception {
        AppUser owner = user(1L);
        Category food = new Category(
                "Food", CategoryType.EXPENSE, null, false
        );

        when(users.findByUsernameIgnoreCase("u@example.com"))
                .thenReturn(Optional.of(owner));
        when(categories.findByNameIgnoreCaseAndOwner_Id("Food", 1L))
                .thenReturn(Optional.empty());
        when(categories.findByNameIgnoreCaseAndOwnerIsNull("Food"))
                .thenReturn(Optional.of(food));

        when(transactions.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            setId(t, 3L);
            return t;
        });

        var view = service.createTransaction(
                "u@example.com",
                new Requests.TransactionCreate(
                        new BigDecimal("12.50"),
                        LocalDate.now(),
                        "Food",
                        "lunch"
                )
        );

        assertEquals(3L, view.id());
        assertEquals("Food", view.category());
        assertEquals("EXPENSE", view.type());
    }

    // 10. Reversed transaction date range is rejected
    @Test
    void listTransactionsRejectsReversedDateRange() throws Exception {
        when(users.findByUsernameIgnoreCase("u@example.com"))
                .thenReturn(Optional.of(user(1L)));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.listTransactions(
                        "u@example.com",
                        LocalDate.now(),
                        LocalDate.now().minusDays(1),
                        null,
                        null
                ));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    // 11. Transactions can be filtered by type and date
    @Test
    void listTransactionsFiltersByTypeAndDate() throws Exception {
        AppUser owner = user(1L);
        Category food = new Category(
                "Food", CategoryType.EXPENSE, null, false
        );
        Category salary = new Category(
                "Salary", CategoryType.INCOME, null, false
        );

        Transaction expense = new Transaction(
                new BigDecimal("10.00"), LocalDate.now(),
                food, owner, "food"
        );
        setId(expense, 2L);

        Transaction income = new Transaction(
                new BigDecimal("100.00"), LocalDate.now(),
                salary, owner, "pay"
        );
        setId(income, 1L);

        when(users.findByUsernameIgnoreCase("u@example.com"))
                .thenReturn(Optional.of(owner));
        when(transactions.findByOwner_IdOrderByDateDescIdDesc(1L))
                .thenReturn(List.of(income, expense));

        var result = service.listTransactions(
                "u@example.com",
                LocalDate.now(),
                LocalDate.now(),
                null,
                "expense"
        );

        assertEquals(1, result.size());
        assertEquals("Food", result.get(0).category());
    }

    // 12. Updating a transaction does not change its date
    @Test
    void updateTransactionDoesNotChangeDate() throws Exception {
        AppUser owner = user(1L);

        Transaction t = new Transaction(
                new BigDecimal("10.00"),
                LocalDate.of(2025, 1, 1),
                new Category("Food", CategoryType.EXPENSE, null, false),
                owner,
                "old"
        );
        setId(t, 9L);

        when(users.findByUsernameIgnoreCase("u@example.com"))
                .thenReturn(Optional.of(owner));
        when(transactions.findByIdAndOwner_Id(9L, 1L))
                .thenReturn(Optional.of(t));

        var view = service.updateTransaction(
                "u@example.com",
                9L,
                new Requests.TransactionUpdate(
                        new BigDecimal("15.00"), "new", null
                )
        );

        assertEquals(new BigDecimal("15.00"), view.amount());
        assertEquals("new", view.description());
        assertEquals(LocalDate.of(2025, 1, 1), view.date());
    }

    // 13. Updating a missing transaction returns not found
    @Test
    void updateMissingTransactionReturnsNotFound() throws Exception {
        when(users.findByUsernameIgnoreCase("u@example.com"))
                .thenReturn(Optional.of(user(1L)));
        when(transactions.findByIdAndOwner_Id(44L, 1L))
                .thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.updateTransaction(
                        "u@example.com",
                        44L,
                        new Requests.TransactionUpdate(null, null, null)
                ));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    // 14. Goal target date must be in the future
    @Test
    void createGoalRequiresFutureTargetDate() {
        var request = new Requests.GoalCreate(
                "Emergency fund",
                new BigDecimal("1000"),
                LocalDate.now(),
                null
        );

        ApiException ex = assertThrows(ApiException.class,
                () -> service.createGoal("u@example.com", request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    // 15. Goal defaults start date and calculates empty progress
    @Test
    void createGoalDefaultsStartDateAndCalculatesEmptyProgress()
            throws Exception {
        AppUser owner = user(1L);

        when(users.findByUsernameIgnoreCase("u@example.com"))
                .thenReturn(Optional.of(owner));
        when(goals.save(any(SavingsGoal.class))).thenAnswer(inv -> {
            SavingsGoal g = inv.getArgument(0);
            setId(g, 5L);
            return g;
        });
        when(transactions.findByOwner_IdAndDateGreaterThanEqual(
                eq(1L), any(LocalDate.class)))
                .thenReturn(List.of());

        var view = service.createGoal(
                "u@example.com",
                new Requests.GoalCreate(
                        "Emergency fund",
                        new BigDecimal("1000.00"),
                        LocalDate.now().plusMonths(1),
                        null
                )
        );

        assertEquals(5L, view.id());
        assertEquals(BigDecimal.ZERO.setScale(2), view.currentProgress());
        assertEquals(new BigDecimal("1000.00"), view.remainingAmount());
    }

    // 16. Monthly report aggregates income, expenses and net savings
    @Test
    void monthlyReportAggregatesIncomeExpensesAndNetSavings()
            throws Exception {
        AppUser owner = user(1L);
        Category salary = new Category(
                "Salary", CategoryType.INCOME, null, false
        );
        Category food = new Category(
                "Food", CategoryType.EXPENSE, null, false
        );

        when(users.findByUsernameIgnoreCase("u@example.com"))
                .thenReturn(Optional.of(owner));
        when(transactions.findByOwner_IdAndDateBetween(
                1L,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31)
        )).thenReturn(List.of(
                new Transaction(
                        new BigDecimal("100.00"),
                        LocalDate.of(2026, 5, 2),
                        salary, owner, "salary"
                ),
                new Transaction(
                        new BigDecimal("25.00"),
                        LocalDate.of(2026, 5, 3),
                        food, owner, "food"
                )
        ));

        var report = service.monthlyReport("u@example.com", 2026, 5);

        assertEquals(new BigDecimal("75.00"), report.get("netSavings"));
        assertTrue(report.containsKey("totalIncome"));
        assertTrue(report.containsKey("totalExpenses"));
    }

    // 17. Invalid monthly report month is rejected
    @Test
    void monthlyReportRejectsInvalidMonth() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.monthlyReport("u@example.com", 2026, 13));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    // 18. Deleting a nonexistent custom category returns not found
    @Test
    void deleteCustomCategoryNotFoundReturnsNotFound() throws Exception {
        when(users.findByUsernameIgnoreCase("u@example.com"))
                .thenReturn(Optional.of(user(1L)));
        when(categories.findByNameIgnoreCaseAndOwnerIsNull("Travel"))
                .thenReturn(Optional.empty());
        when(categories.findByNameIgnoreCaseAndOwner_Id("Travel", 1L))
                .thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.deleteCategory("u@example.com", "Travel"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    // 19. A category referenced by transactions cannot be deleted
    @Test
    void deleteCategoryReferencedByTransactionReturnsBadRequest()
            throws Exception {
        AppUser owner = user(1L);
        Category category = new Category(
                "Travel", CategoryType.EXPENSE, owner, true
        );
        setId(category, 10L);

        when(users.findByUsernameIgnoreCase("u@example.com"))
                .thenReturn(Optional.of(owner));
        when(categories.findByNameIgnoreCaseAndOwnerIsNull("Travel"))
                .thenReturn(Optional.empty());
        when(categories.findByNameIgnoreCaseAndOwner_Id("Travel", 1L))
                .thenReturn(Optional.of(category));
        when(transactions.existsByCategory_Id(10L)).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.deleteCategory("u@example.com", "Travel"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(categories, never()).delete(any(Category.class));
    }

    // 20. Creating a transaction with an invalid category is rejected
    @Test
    void createTransactionWithInvalidCategoryReturnsBadRequest()
            throws Exception {
        AppUser owner = user(1L);

        when(users.findByUsernameIgnoreCase("u@example.com"))
                .thenReturn(Optional.of(owner));
        when(categories.findByNameIgnoreCaseAndOwner_Id("Unknown", 1L))
                .thenReturn(Optional.empty());
        when(categories.findByNameIgnoreCaseAndOwnerIsNull("Unknown"))
                .thenReturn(Optional.empty());

        var request = new Requests.TransactionCreate(
                new BigDecimal("50.00"),
                LocalDate.now(),
                "Unknown",
                "Test"
        );

        ApiException ex = assertThrows(ApiException.class,
                () -> service.createTransaction("u@example.com", request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(transactions, never()).save(any(Transaction.class));
    }

    // 21. Invalid transaction type is rejected
    @Test
    void listTransactionsRejectsInvalidType() throws Exception {
        when(users.findByUsernameIgnoreCase("u@example.com"))
                .thenReturn(Optional.of(user(1L)));
        when(transactions.findByOwner_IdOrderByDateDescIdDesc(1L))
                .thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.listTransactions(
                        "u@example.com", null, null, null, "INVALID"
                ));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    // 22. Deleting a missing transaction returns not found
    @Test
    void deleteMissingTransactionReturnsNotFound() throws Exception {
        when(users.findByUsernameIgnoreCase("u@example.com"))
                .thenReturn(Optional.of(user(1L)));
        when(transactions.findByIdAndOwner_Id(99L, 1L))
                .thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.deleteTransaction("u@example.com", 99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    // 23. Getting a missing goal returns not found
    @Test
    void getMissingGoalReturnsNotFound() throws Exception {
        when(users.findByUsernameIgnoreCase("u@example.com"))
                .thenReturn(Optional.of(user(1L)));
        when(goals.findByIdAndOwner_Id(99L, 1L))
                .thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.getGoal("u@example.com", 99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    // 24. Deleting a missing goal returns not found
    @Test
    void deleteMissingGoalReturnsNotFound() throws Exception {
        when(users.findByUsernameIgnoreCase("u@example.com"))
                .thenReturn(Optional.of(user(1L)));
        when(goals.findByIdAndOwner_Id(99L, 1L))
                .thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.deleteGoal("u@example.com", 99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    // 25. Future goal start date is rejected before repository access
    @Test
    void createGoalRejectsFutureStartDate() {
        var request = new Requests.GoalCreate(
                "Emergency Fund",
                new BigDecimal("1000.00"),
                LocalDate.now().plusMonths(2),
                LocalDate.now().plusDays(1)
        );

        ApiException ex = assertThrows(ApiException.class,
                () -> service.createGoal("u@example.com", request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verifyNoInteractions(users, goals);
    }

    // 26. Invalid yearly report year is rejected
    @Test
    void yearlyReportRejectsInvalidYear() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.yearlyReport("u@example.com", 1800));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verifyNoInteractions(users, transactions);
    }

    // 27. Start date after end date is rejected with the expected message
    @Test
    void listTransactionsRejectsStartDateAfterEndDate() throws Exception {
        when(users.findByUsernameIgnoreCase("u@example.com"))
                .thenReturn(Optional.of(user(1L)));

        LocalDate start = LocalDate.now();
        LocalDate end = start.minusDays(1);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.listTransactions(
                        "u@example.com", start, end, null, null
                ));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(
                "startDate must not be after endDate",
                ex.getMessage()
        );
    }

    // 28. Invalid transaction type reports the expected message
    @Test
    void listTransactionsInvalidTypeReturnsExpectedMessage()
            throws Exception {
        when(users.findByUsernameIgnoreCase("u@example.com"))
                .thenReturn(Optional.of(user(1L)));
        when(transactions.findByOwner_IdOrderByDateDescIdDesc(1L))
                .thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.listTransactions(
                        "u@example.com", null, null, null, "TRANSFER"
                ));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("type must be INCOME or EXPENSE", ex.getMessage());
    }

    // 29. Month zero is rejected
    @Test
    void monthlyReportRejectsMonthZero() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.monthlyReport("u@example.com", 2026, 0));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    // 30. Month thirteen is rejected
    @Test
    void monthlyReportRejectsMonthThirteen() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.monthlyReport("u@example.com", 2026, 13));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    // 31. Year immediately below the supported range is rejected
    @Test
    void yearlyReportRejectsYearBefore1900() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.yearlyReport("u@example.com", 1899));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    // 32. Year above the supported range is rejected
    @Test
    void yearlyReportRejectsYearAbove9999() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.yearlyReport("u@example.com", 10000));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    // 33. A goal start date after its target date is rejected
    @Test
    void createGoalRejectsStartDateAfterTargetDate() {
        LocalDate today = LocalDate.now();

        Requests.GoalCreate request = new Requests.GoalCreate(
                "Emergency Fund",
                new BigDecimal("1000.00"),
                today.plusDays(5),
                today.plusDays(6)
        );

        ApiException ex = assertThrows(ApiException.class,
                () -> service.createGoal("u@example.com", request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(
                "Start date must not be after target date",
                ex.getMessage()
        );
        verifyNoInteractions(users, goals);
    }

    // 34. A goal target date before its start date is rejected
    @Test
    void updateGoalRejectsTargetDateBeforeStartDate() throws Exception {
        AppUser owner = user(1L);
        LocalDate start = LocalDate.now().minusDays(5);

        SavingsGoal goal = new SavingsGoal(
                "Emergency Fund",
                new BigDecimal("1000.00"),
                LocalDate.now().plusMonths(2),
                start,
                owner
        );
        setId(goal, 5L);

        when(users.findByUsernameIgnoreCase("u@example.com"))
                .thenReturn(Optional.of(owner));
        when(goals.findByIdAndOwner_Id(5L, 1L))
                .thenReturn(Optional.of(goal));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.updateGoal(
                        "u@example.com",
                        5L,
                        new Requests.GoalUpdate(null, start.minusDays(1))
                ));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(
                "Target date must not be before start date",
                ex.getMessage()
        );
    }

    // 35. A goal target date today is rejected during update
    @Test
    void updateGoalRejectsTargetDateThatIsNotInFuture() throws Exception {
        AppUser owner = user(1L);
        LocalDate today = LocalDate.now();

        SavingsGoal goal = new SavingsGoal(
                "Emergency Fund",
                new BigDecimal("1000.00"),
                today.plusMonths(2),
                today.minusDays(5),
                owner
        );
        setId(goal, 5L);

        when(users.findByUsernameIgnoreCase("u@example.com"))
                .thenReturn(Optional.of(owner));
        when(goals.findByIdAndOwner_Id(5L, 1L))
                .thenReturn(Optional.of(goal));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.updateGoal(
                        "u@example.com",
                        5L,
                        new Requests.GoalUpdate(null, today)
                ));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(
                "Target date must be in the future",
                ex.getMessage()
        );
    }

    // Helper: create a test user with a known ID
    private static AppUser user(long id) throws Exception {
        AppUser u = new AppUser();
        setId(u, id);
        u.setUsername("u@example.com");
        return u;
    }

    // Helper: set entity ID using reflection
    private static void setId(Object object, long id) throws Exception {
        Field field = object.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(object, id);
    }
}
