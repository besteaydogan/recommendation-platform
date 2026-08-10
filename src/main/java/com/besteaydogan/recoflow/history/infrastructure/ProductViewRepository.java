package com.besteaydogan.recoflow.history.infrastructure;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductViewRepository extends JpaRepository<ProductView, Long> {

    boolean existsByMessageId(UUID messageId);

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
