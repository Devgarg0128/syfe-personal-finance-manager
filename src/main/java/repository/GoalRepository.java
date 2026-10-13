package com.syfe.finance.repository;
import com.syfe.finance.entity.SavingsGoal; import org.springframework.data.jpa.repository.JpaRepository; import java.util.*;
public interface GoalRepository extends JpaRepository<SavingsGoal,Long>{ List<SavingsGoal> findByOwner_IdOrderByIdDesc(Long ownerId); Optional<SavingsGoal> findByIdAndOwner_Id(Long id,Long ownerId); }
