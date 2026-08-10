package com.besteaydogan.recoflow.history.infrastructure;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "product_views",
        schema = "public",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_product_views_message_id",
                columnNames = "message_id"
        )
)
public class ProductView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "message_id", nullable = false, updatable = false)
    private UUID messageId;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "product_id", nullable = false, length = 50)
    private String productId;

    @Column(name = "source", nullable = false, length = 50)
    private String source;

    @Column(name = "viewed_at", nullable = false)
    private Instant viewedAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected ProductView() {
    }

    public ProductView(UUID messageId, String userId, String productId, String source, Instant viewedAt) {
        this.messageId = messageId;
        this.userId = userId;
        this.productId = productId;
        this.source = source;
        this.viewedAt = viewedAt;
    }

    public Long getId() {
        return id;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public String getUserId() {
        return userId;
    }

    public String getProductId() {
        return productId;
    }

    public String getSource() {
        return source;
    }

    public Instant getViewedAt() {
        return viewedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
