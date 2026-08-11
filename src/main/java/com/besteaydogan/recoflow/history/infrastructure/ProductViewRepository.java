package com.besteaydogan.recoflow.history.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductViewRepository extends JpaRepository<ProductView, Long> {

    @Modifying
    @Query(value = """
            insert into product_views (message_id, user_id, product_id, source, viewed_at)
            values (:messageId, :userId, :productId, :source, :viewedAt)
            on conflict (message_id) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("messageId") UUID messageId,
            @Param("userId") String userId,
            @Param("productId") String productId,
            @Param("source") String source,
            @Param("viewedAt") Instant viewedAt
    );

    List<ProductView> findTop10ByUserIdOrderByViewedAtDescIdDesc(String userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from ProductView productView
            where productView.userId = :userId
              and productView.productId = :productId
            """)
    int deleteAllByUserIdAndProductId(
            @Param("userId") String userId,
            @Param("productId") String productId
    );
}
