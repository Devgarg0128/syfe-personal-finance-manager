package com.syfe.finance.repository;
import com.syfe.finance.entity.Transaction; import org.springframework.data.jpa.repository.JpaRepository; import java.time.LocalDate; import java.util.*;
public interface TransactionRepository extends JpaRepository<Transaction,Long>{
 List<Transaction> findByOwner_IdOrderByDateDescIdDesc(Long ownerId);
 List<Transaction> findByOwner_IdAndDateBetweenOrderByDateDescIdDesc(Long ownerId,LocalDate start,LocalDate end);
 List<Transaction> findByOwner_IdAndCategory_IdOrderByDateDescIdDesc(Long ownerId,Long categoryId);
 Optional<Transaction> findByIdAndOwner_Id(Long id,Long ownerId);
 boolean existsByCategory_Id(Long categoryId);
 List<Transaction> findByOwner_IdAndDateGreaterThanEqual(Long ownerId,LocalDate startDate);
 List<Transaction> findByOwner_IdAndDateBetween(Long ownerId,LocalDate start,LocalDate end);
}
