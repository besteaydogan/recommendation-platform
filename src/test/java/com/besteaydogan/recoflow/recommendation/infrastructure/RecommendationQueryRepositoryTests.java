package com.besteaydogan.recoflow.recommendation.infrastructure;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.besteaydogan.recoflow.history.infrastructure.TopCategoryQueryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TopCategoryQueryRepository.class, BestsellerQueryRepository.class})
class RecommendationQueryRepositoryTests {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:17.9-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TopCategoryQueryRepository categoryRepository;

    @Autowired
    private BestsellerQueryRepository bestsellerRepository;

    @Test
    void assignmentMonthlyOrderDatasetHasNoTimestampColumn() {
        List<String> columns = jdbcTemplate.queryForList(
                """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'orders'
                ORDER BY ordinal_position
                """,
                String.class
        );

        assertThat(columns).containsExactly("order_id", "user_id");
    }

    @Test
    void topCategoriesUseViewCountThenRecencyAndReturnAtMostThree() {
        insertProduct("product-a", "category-a");
        insertProduct("product-b", "category-b");
        insertProduct("product-c", "category-c");
        insertProduct("product-d", "category-d");
        Instant base = Instant.parse("2026-07-29T10:00:00Z");
        insertViews("user-120", "product-a", base, base.plusSeconds(1), base.plusSeconds(2));
        insertViews("user-120", "product-b", base, base.plusSeconds(1), base.plusSeconds(3));
        insertViews("user-120", "product-c", base, base.plusSeconds(10));
        insertViews("user-120", "product-d", base.plusSeconds(20));
        insertViews("other-user", "product-d", base.plusSeconds(30), base.plusSeconds(31));

        List<String> categories = categoryRepository.findTopCategories("user-120", 3);

        assertThat(categories).containsExactly("category-b", "category-a", "category-c");
    }

    @Test
    void generalRankingOverProvidedMonthlyDatasetCountsDistinctBuyersAndLimitsToTen() {
        seedProvidedMonthlyOrderDataset();

        List<BestsellerRow> result = bestsellerRepository.findGeneralBestsellers(10);

        assertThat(result).hasSize(10);
        assertThat(result).extracting(BestsellerRow::productId)
                .containsExactly(
                        "product-01", "product-02", "product-03", "product-04", "product-05",
                        "product-06", "product-07", "product-08", "product-09", "product-10"
                );
        assertThat(result.getFirst().distinctBuyerCount()).isEqualTo(12);
        assertThat(result.get(1).distinctBuyerCount()).isEqualTo(11);
        assertThat(result.get(2).distinctBuyerCount()).isEqualTo(11);
    }

    @Test
    void categoryRankingFiltersSelectedCategoriesAndRanksOneCombinedResult() {
        seedProvidedMonthlyOrderDataset();

        List<BestsellerRow> categoryB =
                bestsellerRepository.findBestsellersForCategories(List.of("category-b"), 10);
        List<BestsellerRow> combined =
                bestsellerRepository.findBestsellersForCategories(
                        List.of("category-a", "category-b"), 10);

        assertThat(categoryB).extracting(BestsellerRow::productId)
                .containsExactly(
                        "product-07", "product-08", "product-09",
                        "product-10", "product-11", "product-12"
                );
        assertThat(combined).extracting(BestsellerRow::productId)
                .containsExactly(
                        "product-01", "product-02", "product-03", "product-04", "product-05",
                        "product-06", "product-07", "product-08", "product-09", "product-10"
                );
    }

    private void seedProvidedMonthlyOrderDataset() {
        int[] buyerCounts = {12, 11, 11, 9, 8, 7, 6, 5, 4, 3, 2, 1};
        long orderId = 1;
        for (int productIndex = 1; productIndex <= 12; productIndex++) {
            String productId = "product-%02d".formatted(productIndex);
            String categoryId = productIndex <= 6 ? "category-a" : "category-b";
            insertProduct(productId, categoryId);
            for (int buyer = 1; buyer <= buyerCounts[productIndex - 1]; buyer++) {
                insertOrder(orderId++, "buyer-" + buyer, productId);
            }
        }
        insertOrder(orderId, "buyer-1", "product-01");
    }

    private void insertProduct(String productId, String categoryId) {
        jdbcTemplate.update(
                "INSERT INTO products(product_id, category_id) VALUES (?, ?)",
                productId,
                categoryId
        );
    }

    private void insertOrder(long orderId, String userId, String productId) {
        jdbcTemplate.update(
                "INSERT INTO orders(order_id, user_id) VALUES (?, ?)",
                orderId,
                userId
        );
        jdbcTemplate.update(
                "INSERT INTO order_items(product_id, quantity, order_id) VALUES (?, ?, ?)",
                productId,
                1,
                orderId
        );
    }

    private void insertViews(String userId, String productId, Instant... viewedAtValues) {
        for (Instant viewedAt : viewedAtValues) {
            jdbcTemplate.update(
                    """
                    INSERT INTO product_views(
                        message_id, user_id, product_id, source, viewed_at
                    ) VALUES (?, ?, ?, ?, ?)
                    """,
                    UUID.randomUUID(),
                    userId,
                    productId,
                    "test",
                    Timestamp.from(viewedAt)
            );
        }
    }
}
