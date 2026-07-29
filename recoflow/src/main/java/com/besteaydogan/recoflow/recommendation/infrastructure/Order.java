package com.besteaydogan.recoflow.recommendation.infrastructure;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders", schema = "public")
public class Order {

    @Id
    @Column(name = "order_id", nullable = false, updatable = false)
    private BigDecimal id;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    protected Order() {
    }

    public Order(BigDecimal id, String userId) {
        this.id = id;
        this.userId = userId;
    }

    public BigDecimal getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }
}
