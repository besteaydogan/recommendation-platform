package com.besteaydogan.recoflow.history.infrastructure;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TopCategoryQueryRepository {

    private static final String TOP_CATEGORIES_SQL = """
            SELECT product.category_id
            FROM product_views product_view
            JOIN products product
              ON product.product_id = product_view.product_id
            WHERE product_view.user_id = :userId
            GROUP BY product.category_id
            ORDER BY COUNT(*) DESC,
                     MAX(product_view.viewed_at) DESC,
                     product.category_id ASC
            LIMIT :limit
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public TopCategoryQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> findTopCategories(String userId, int limit) {
        return jdbcTemplate.queryForList(
                TOP_CATEGORIES_SQL,
                Map.of("userId", userId, "limit", limit),
                String.class
        );
    }
}
