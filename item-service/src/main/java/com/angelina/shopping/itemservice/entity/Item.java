package com.angelina.shopping.itemservice.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer priceCents;

    protected Item() {}

    public Item(String name, Integer priceCents) {
        this.name = name;
        this.priceCents = priceCents;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Integer getPriceCents() { return priceCents; }

    public void setName(String name) { this.name = name; }
    public void setPriceCents(Integer priceCents) { this.priceCents = priceCents; }
}