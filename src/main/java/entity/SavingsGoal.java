package com.syfe.finance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
@Entity @Table(name="savings_goals")
public class SavingsGoal {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, length=120) private String goalName;
    @Column(nullable=false, precision=19, scale=2) private BigDecimal targetAmount;
    @Column(nullable=false) private LocalDate targetDate;
    @Column(nullable=false) private LocalDate startDate;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="owner_id", nullable=false) private AppUser owner;
    protected SavingsGoal(){}
    public SavingsGoal(String n, BigDecimal a, LocalDate t, LocalDate s, AppUser o){goalName=n;targetAmount=a;targetDate=t;startDate=s;owner=o;}
    public Long getId(){return id;} public String getGoalName(){return goalName;} public BigDecimal getTargetAmount(){return targetAmount;} public void setTargetAmount(BigDecimal v){targetAmount=v;}
    public LocalDate getTargetDate(){return targetDate;} public void setTargetDate(LocalDate v){targetDate=v;} public LocalDate getStartDate(){return startDate;} public AppUser getOwner(){return owner;}
}
