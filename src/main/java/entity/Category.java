package com.syfe.finance.entity;

import jakarta.persistence.*;
@Entity @Table(name="categories", uniqueConstraints=@UniqueConstraint(columnNames={"owner_id","name"}))
public class Category {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, length=80) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=10) private CategoryType type;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="owner_id") private AppUser owner;
    @Column(nullable=false) private boolean isCustom;
    protected Category(){}
    public Category(String name, CategoryType type, AppUser owner, boolean isCustom){this.name=name;this.type=type;this.owner=owner;this.isCustom=isCustom;}
    public Long getId(){return id;} public String getName(){return name;} public CategoryType getType(){return type;} public AppUser getOwner(){return owner;} public boolean isCustom(){return isCustom;}
}
