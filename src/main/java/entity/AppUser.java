package com.syfe.finance.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="app_users", uniqueConstraints=@UniqueConstraint(columnNames="username"))
public class AppUser {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true, length=254) private String username;
    @Column(nullable=false) private String passwordHash;
    @Column(nullable=false) private String fullName;
    @Column(nullable=false, length=30) private String phoneNumber;
    @Column(nullable=false) private Instant createdAt=Instant.now();
    public Long getId(){return id;} public String getUsername(){return username;} public void setUsername(String v){username=v;}
    public String getPasswordHash(){return passwordHash;} public void setPasswordHash(String v){passwordHash=v;}
    public String getFullName(){return fullName;} public void setFullName(String v){fullName=v;}
    public String getPhoneNumber(){return phoneNumber;} public void setPhoneNumber(String v){phoneNumber=v;}
    public Instant getCreatedAt(){return createdAt;}
}
