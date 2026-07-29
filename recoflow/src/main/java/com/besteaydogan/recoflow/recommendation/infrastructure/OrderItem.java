package com.besteaydogan.recoflow.recommendation.infrastructure;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_items", schema = "public")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Integer id;

    @Column(name = "order_id", nullable = false)
    private BigDecimal orderId;

    @Column(name = "product_id", nullable = false, length = 50)
    private String productId;

    @Column(name = "quantity", nullable = false)
    private BigDecimal quantity;

    protected OrderItem() {
    }

    public OrderItem(BigDecimal orderId, String productId, BigDecimal quantity) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
    }

    public Integer getId() {
        return id;
    }

    public BigDecimal getOrderId() {
        return orderId;
    }

    public String getProductId() {
        return productId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }
}
