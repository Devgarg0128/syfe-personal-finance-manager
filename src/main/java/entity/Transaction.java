package com.syfe.finance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
@Entity @Table(name="transactions", indexes={@Index(columnList="owner_id,date")})
public class Transaction {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, precision=19, scale=2) private BigDecimal amount;
    @Column(nullable=false) private LocalDate date;
    @ManyToOne(fetch=FetchType.EAGER, optional=false) @JoinColumn(name="category_id", nullable=false) private Category category;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="owner_id", nullable=false) private AppUser owner;
    @Column(length=500) private String description;
    protected Transaction(){}
    public Transaction(BigDecimal amount, LocalDate date, Category category, AppUser owner, String description){this.amount=amount;this.date=date;this.category=category;this.owner=owner;this.description=description;}
    public Long getId(){return id;} public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
    public LocalDate getDate(){return date;} public Category getCategory(){return category;} public void setCategory(Category v){category=v;}
    public AppUser getOwner(){return owner;} public String getDescription(){return description;} public void setDescription(String v){description=v;}
}
