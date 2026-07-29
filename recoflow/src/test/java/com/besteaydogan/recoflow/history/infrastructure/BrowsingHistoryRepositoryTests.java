package com.besteaydogan.recoflow.history.infrastructure;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BrowsingHistoryRepositoryTests {

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
    private ProductViewRepository repository;

    @Test
    void returnsLatestTenForOnlyRequestedUserInDescendingOrderWithRepeats() {
        Instant base = Instant.parse("2026-07-29T10:00:00Z");
        List<ProductView> views = new ArrayList<>();
        for (int index = 0; index < 12; index++) {
            String productId = index >= 10 ? "product-repeat" : "product-" + index;
            views.add(view("user-120", productId, base.plusSeconds(index)));
        }
        views.add(view("user-other", "product-newer", base.plusSeconds(100)));
        repository.saveAllAndFlush(views);

        List<ProductView> result =
                repository.findTop10ByUserIdOrderByViewedAtDescIdDesc("user-120");

        assertThat(result).hasSize(10);
        assertThat(result).extracting(ProductView::getUserId).containsOnly("user-120");
        assertThat(result).extracting(ProductView::getViewedAt).isSortedAccordingTo(
                (left, right) -> right.compareTo(left)
        );
        assertThat(result).extracting(ProductView::getProductId)
                .startsWith("product-repeat", "product-repeat");
    }

    @Test
    void bulkDeleteRemovesAllMatchingRowsWithoutTouchingOtherHistory() {
        repository.saveAllAndFlush(List.of(
                view("user-120", "product-10", Instant.parse("2026-07-29T10:00:00Z")),
                view("user-120", "product-10", Instant.parse("2026-07-29T10:01:00Z")),
                view("user-120", "product-10", Instant.parse("2026-07-29T10:02:00Z")),
                view("user-other", "product-10", Instant.parse("2026-07-29T10:03:00Z")),
                view("user-120", "product-other", Instant.parse("2026-07-29T10:04:00Z"))
        ));

        int deleted = repository.deleteAllByUserIdAndProductId("user-120", "product-10");

        assertThat(deleted).isEqualTo(3);
        assertThat(repository.findAll())
                .extracting(ProductView::getUserId, ProductView::getProductId)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("user-other", "product-10"),
                        org.assertj.core.groups.Tuple.tuple("user-120", "product-other")
                );
    }

    @Test
    void bulkDeleteReturnsZeroWhenNothingMatches() {
        int deleted = repository.deleteAllByUserIdAndProductId("missing-user", "missing-product");

        assertThat(deleted).isZero();
    }

    private ProductView view(String userId, String productId, Instant viewedAt) {
        return new ProductView(UUID.randomUUID(), userId, productId, "test", viewedAt);
    }
}
