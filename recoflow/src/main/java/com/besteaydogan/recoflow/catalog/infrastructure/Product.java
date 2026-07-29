package com.besteaydogan.recoflow.catalog.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "products", schema = "public")
public class Product {

    @Id
    @Column(name = "product_id", nullable = false, length = 50, updatable = false)
    private String id;

    @Column(name = "category_id", nullable = false, length = 50)
    private String categoryId;

    protected Product() {
    }

    public Product(String id, String categoryId) {
        this.id = id;
        this.categoryId = categoryId;
    }

    public String getId() {
        return id;
    }

    public String getCategoryId() {
        return categoryId;
    }
}
