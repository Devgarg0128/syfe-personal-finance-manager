
package com.syfe.finance.service;

import com.syfe.finance.dto.*;
import com.syfe.finance.entity.*;
import com.syfe.finance.exception.ApiException;
import com.syfe.finance.repository.*;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class FinanceService {

    private final UserRepository users;
    private final CategoryRepository categories;
    private final TransactionRepository transactions;
    private final GoalRepository goals;
    private final PasswordEncoder encoder;

    public FinanceService(
            UserRepository u,
            CategoryRepository c,
            TransactionRepository t,
            GoalRepository g,
            PasswordEncoder e) {
        users = u;
        categories = c;
        transactions = t;
        goals = g;
        encoder = e;
    }

    public Map<String, Object> register(Requests.Register r) {
        String email = r.username().trim().toLowerCase(Locale.ROOT);

        if (users.existsByUsernameIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Username already exists");
        }

        AppUser u = new AppUser();
        u.setUsername(email);
        u.setPasswordHash(encoder.encode(r.password()));
        u.setFullName(r.fullName().trim());
        u.setPhoneNumber(r.phoneNumber().trim());

        users.save(u);

        return Map.of(
            "message", "User registered successfully",
            "userId", u.getId()
        );
    }

    public AppUser user(String username) {
        return users.findByUsernameIgnoreCase(username)
            .orElseThrow(() ->
                new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
    }

    public List<Views.CategoryView> listCategories(String username) {
        AppUser u = user(username);

        return categories.findByOwner_IdOrOwnerIsNull(u.getId())
            .stream()
            .sorted(Comparator.comparing(
                Category::getName,
                String.CASE_INSENSITIVE_ORDER
            ))
            .map(Views.CategoryView::of)
            .toList();
    }

    public Views.CategoryView createCategory(
            String username,
            Requests.CategoryCreate r) {

        AppUser u = user(username);
        String name = r.name().trim();

        if (categories.existsByNameIgnoreCaseAndOwner_Id(name, u.getId())
                || categories.existsByNameIgnoreCaseAndOwnerIsNull(name)) {
            throw new ApiException(HttpStatus.CONFLICT, "Category name already exists");
        }

        return Views.CategoryView.of(
            categories.save(new Category(name, r.type(), u, true))
        );
    }

    public Map<String, String> deleteCategory(String username, String name) {
        AppUser u = user(username);

        if (categories.findByNameIgnoreCaseAndOwnerIsNull(name).isPresent()) {
            throw new ApiException(
                HttpStatus.FORBIDDEN,
                "Default categories cannot be deleted"
            );
        }

        Category c = categories.findByNameIgnoreCaseAndOwner_Id(name, u.getId())
            .orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "Custom category not found"));

        if (transactions.existsByCategory_Id(c.getId())) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "Category is referenced by transactions and cannot be deleted"
            );
        }

        categories.delete(c);

        return Map.of("message", "Category deleted successfully");
    }

    private Category category(String username, String name) {
        AppUser u = user(username);

        return categories.findByNameIgnoreCaseAndOwner_Id(name, u.getId())
            .or(() -> categories.findByNameIgnoreCaseAndOwnerIsNull(name))
            .orElseThrow(() ->
                new ApiException(HttpStatus.BAD_REQUEST, "Invalid category"));
    }

    public Views.TransactionView createTransaction(
            String username,
            Requests.TransactionCreate r) {

        if (r.date().isAfter(LocalDate.now())) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "Transaction date cannot be in the future"
            );
        }

        Category c = category(username, r.category());

        return Views.TransactionView.of(
            transactions.save(
                new Transaction(r.amount(), r.date(), c, user(username), r.description())
            )
        );
    }

    public List<Views.TransactionView> listTransactions(
            String username,
            LocalDate start,
            LocalDate end,
            Long categoryId,
            String type) {

        AppUser u = user(username);

        if (start != null && end != null && start.isAfter(end)) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "startDate must not be after endDate"
            );
        }

        List<Transaction> list =
            transactions.findByOwner_IdOrderByDateDescIdDesc(u.getId());

        if (start != null) {
            list = list.stream()
                .filter(t -> !t.getDate().isBefore(start))
                .toList();
        }

        if (end != null) {
            list = list.stream()
                .filter(t -> !t.getDate().isAfter(end))
                .toList();
        }

        if (categoryId != null) {
            list = list.stream()
                .filter(t -> t.getCategory().getId().equals(categoryId))
                .toList();
        }

        if (type != null && !type.isBlank()) {
            CategoryType ct;

            try {
                ct = CategoryType.valueOf(type.toUpperCase(Locale.ROOT));
            } catch (Exception e) {
                throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "type must be INCOME or EXPENSE"
                );
            }

            list = list.stream()
                .filter(t -> t.getCategory().getType() == ct)
                .toList();
        }

        return list.stream()
            .map(Views.TransactionView::of)
            .toList();
    }

    public Views.TransactionView updateTransaction(
            String username,
            Long id,
            Requests.TransactionUpdate r) {

        AppUser u = user(username);

        Transaction t = transactions.findByIdAndOwner_Id(id, u.getId())
            .orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "Transaction not found"));

        if (r.amount() != null) {
            t.setAmount(r.amount());
        }

        if (r.description() != null) {
            t.setDescription(r.description());
        }

        if (r.category() != null) {
            t.setCategory(category(username, r.category()));
        }

        return Views.TransactionView.of(t);
    }

    public Map<String, String> deleteTransaction(String username, Long id) {
        AppUser u = user(username);

        Transaction t = transactions.findByIdAndOwner_Id(id, u.getId())
            .orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "Transaction not found"));

        transactions.delete(t);

        return Map.of("message", "Transaction deleted successfully");
    }

    public Views.GoalView createGoal(String username, Requests.GoalCreate r) {
        LocalDate today = LocalDate.now();
        LocalDate start = r.startDate() == null ? today : r.startDate();

        if (start.isAfter(r.targetDate())) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "Start date must not be after target date"
            );
        }

        if (!r.targetDate().isAfter(today)) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "Target date must be in the future"
            );
        }

        if (start.isAfter(today)) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "Start date cannot be in the future"
            );
        }

        SavingsGoal g = goals.save(
            new SavingsGoal(
                r.goalName().trim(),
                r.targetAmount(),
                r.targetDate(),
                start,
                user(username)
            )
        );

        return goalView(g);
    }

    public List<Views.GoalView> listGoals(String username) {
        AppUser u = user(username);

        return goals.findByOwner_IdOrderByIdDesc(u.getId())
            .stream()
            .map(this::goalView)
            .toList();
    }

    public Views.GoalView getGoal(String username, Long id) {
        AppUser u = user(username);

        return goalView(
            goals.findByIdAndOwner_Id(id, u.getId())
                .orElseThrow(() ->
                    new ApiException(HttpStatus.NOT_FOUND, "Goal not found"))
        );
    }

    public Views.GoalView updateGoal(
            String username,
            Long id,
            Requests.GoalUpdate r) {

        AppUser u = user(username);

        SavingsGoal g = goals.findByIdAndOwner_Id(id, u.getId())
            .orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "Goal not found"));

        if (r.targetAmount() != null) {
            g.setTargetAmount(r.targetAmount());
        }

        if (r.targetDate() != null) {
            if (r.targetDate().isBefore(g.getStartDate())) {
                throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Target date must not be before start date"
                );
            }

            if (!r.targetDate().isAfter(LocalDate.now())) {
                throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "Target date must be in the future"
                );
            }

            g.setTargetDate(r.targetDate());
        }

        return goalView(g);
    }

    public Map<String, String> deleteGoal(String username, Long id) {
        AppUser u = user(username);

        SavingsGoal g = goals.findByIdAndOwner_Id(id, u.getId())
            .orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "Goal not found"));

        goals.delete(g);

        return Map.of("message", "Goal deleted successfully");
    }

    private Views.GoalView goalView(SavingsGoal g) {
        List<Transaction> ts =
            transactions.findByOwner_IdAndDateGreaterThanEqual(
                g.getOwner().getId(),
                g.getStartDate()
            );

        BigDecimal progress = ts.stream()
            .map(t -> t.getCategory().getType() == CategoryType.INCOME
                ? t.getAmount()
                : t.getAmount().negate())
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .max(BigDecimal.ZERO);

        BigDecimal pct = progress
            .multiply(BigDecimal.valueOf(100))
            .divide(g.getTargetAmount(), 2, RoundingMode.HALF_UP)
            .min(new BigDecimal("100.00"));

        BigDecimal remaining = g.getTargetAmount()
            .subtract(progress)
            .max(BigDecimal.ZERO);

        return new Views.GoalView(
            g.getId(),
            g.getGoalName(),
            g.getTargetAmount(),
            g.getTargetDate(),
            g.getStartDate(),
            progress.setScale(2, RoundingMode.HALF_UP),
            pct,
            remaining.setScale(2, RoundingMode.HALF_UP)
        );
    }

    public Map<String, Object> monthlyReport(String username, int year, int month) {
        if (month < 1 || month > 12) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "month must be between 1 and 12"
            );
        }

        YearMonth ym = YearMonth.of(year, month);

        return report(
            username,
            ym.atDay(1),
            ym.atEndOfMonth(),
            Map.of("month", month, "year", year)
        );
    }

    public Map<String, Object> yearlyReport(String username, int year) {
        if (year < 1900 || year > 9999) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid year");
        }

        return report(
            username,
            LocalDate.of(year, 1, 1),
            LocalDate.of(year, 12, 31),
            Map.of("year", year)
        );
    }

    private Map<String, Object> report(
            String username,
            LocalDate start,
            LocalDate end,
            Map<String, Object> base) {

        AppUser u = user(username);

        Map<String, BigDecimal> income = new TreeMap<>();
        Map<String, BigDecimal> expense = new TreeMap<>();

        for (Transaction t :
                transactions.findByOwner_IdAndDateBetween(u.getId(), start, end)) {

            Map<String, BigDecimal> dest =
                t.getCategory().getType() == CategoryType.INCOME
                    ? income
                    : expense;

            dest.merge(t.getCategory().getName(), t.getAmount(), BigDecimal::add);
        }

        BigDecimal in = income.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal out = expense.values().stream()
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new LinkedHashMap<>(base);
        result.put("totalIncome", income);
        result.put("totalExpenses", expense);
        result.put("netSavings", in.subtract(out).setScale(2, RoundingMode.HALF_UP));

        return result;
    }
}
