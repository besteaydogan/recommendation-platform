package com.besteaydogan.recoflow.recommendation.infrastructure;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class BestsellerQueryRepository {

    private static final String BASE_SQL = """
            SELECT product.product_id,
                   COUNT(DISTINCT customer_order.user_id) AS distinct_buyer_count
            FROM orders customer_order
            JOIN order_items order_item
              ON order_item.order_id = customer_order.order_id
            JOIN products product
              ON product.product_id = order_item.product_id
            """;

    private static final String RANK_AND_LIMIT_SQL = """
            GROUP BY product.product_id
            ORDER BY distinct_buyer_count DESC,
                     product.product_id ASC
            LIMIT :limit
            """;

    private static final String GENERAL_SQL = BASE_SQL + RANK_AND_LIMIT_SQL;

    private static final String CATEGORY_SQL = BASE_SQL + """
            WHERE product.category_id IN (:categoryIds)
            """ + RANK_AND_LIMIT_SQL;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public BestsellerQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<BestsellerRow> findGeneralBestsellers(int limit) {
        return query(GENERAL_SQL, Map.of("limit", limit));
    }

    public List<BestsellerRow> findBestsellersForCategories(List<String> categoryIds, int limit) {
        if (categoryIds.isEmpty()) {
            return List.of();
        }
        return query(CATEGORY_SQL, Map.of("categoryIds", categoryIds, "limit", limit));
    }

    private List<BestsellerRow> query(String sql, Map<String, ?> parameters) {
        return jdbcTemplate.query(
                sql,
                parameters,
                (resultSet, rowNumber) -> new BestsellerRow(
                        resultSet.getString("product_id"),
                        resultSet.getLong("distinct_buyer_count")
                )
        );
    }
}
