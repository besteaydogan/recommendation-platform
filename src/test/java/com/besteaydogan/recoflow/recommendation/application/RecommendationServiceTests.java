package com.besteaydogan.recoflow.recommendation.application;

import java.util.List;
import java.util.stream.IntStream;

import com.besteaydogan.recoflow.history.infrastructure.TopCategoryQueryRepository;
import com.besteaydogan.recoflow.recommendation.api.RecommendationResponse;
import com.besteaydogan.recoflow.recommendation.api.RecommendationType;
import com.besteaydogan.recoflow.recommendation.infrastructure.BestsellerQueryRepository;
import com.besteaydogan.recoflow.recommendation.infrastructure.BestsellerRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RecommendationServiceTests {

    private TopCategoryQueryRepository categoryRepository;
    private BestsellerQueryRepository bestsellerRepository;
    private BestsellerRefreshService refreshService;
    private RecommendationService service;

    @BeforeEach
    void setUp() {
        categoryRepository = mock(TopCategoryQueryRepository.class);
        bestsellerRepository = mock(BestsellerQueryRepository.class);
        refreshService = mock(BestsellerRefreshService.class);
        service = new RecommendationService(categoryRepository, bestsellerRepository, refreshService);
    }

    @Test
    void categoriesUsePersonalizedQueryAndReturnTenProducts() {
        List<String> categories = List.of("category-a", "category-b");
        when(categoryRepository.findTopCategories("user-120", 3)).thenReturn(categories);
        when(bestsellerRepository.findBestsellersForCategories(categories, 10))
                .thenReturn(rows(10));

        RecommendationResponse response = service.recommend("user-120");

        assertThat(response.type()).isEqualTo(RecommendationType.PERSONALIZED);
        assertThat(response.products()).hasSize(10);
        verify(bestsellerRepository).findBestsellersForCategories(categories, 10);
        verifyNoInteractions(refreshService);
    }

    @Test
    void noCategoriesUsesGeneralBestsellers() {
        when(categoryRepository.findTopCategories("missing-user", 3)).thenReturn(List.of());
        when(refreshService.generalBestsellers()).thenReturn(productIds(10));

        RecommendationResponse response = service.recommend("missing-user");

        assertThat(response.type()).isEqualTo(RecommendationType.NON_PERSONALIZED);
        assertThat(response.products()).hasSize(10);
        verify(refreshService).generalBestsellers();
        verify(bestsellerRepository, never())
                .findBestsellersForCategories(org.mockito.ArgumentMatchers.anyList(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void personalizedFiveProductsAreReturned() {
        stubPersonalized(rows(5));

        RecommendationResponse response = service.recommend("user-120");

        assertThat(response.products()).hasSize(5);
        assertThat(response.type()).isEqualTo(RecommendationType.PERSONALIZED);
    }

    @Test
    void personalizedFourProductsReturnEmptyWithoutGeneralFallback() {
        stubPersonalized(rows(4));

        RecommendationResponse response = service.recommend("user-120");

        assertThat(response.products()).isEmpty();
        assertThat(response.type()).isEqualTo(RecommendationType.PERSONALIZED);
        verifyNoInteractions(refreshService);
    }

    @Test
    void generalFewerThanFiveProductsReturnEmpty() {
        when(categoryRepository.findTopCategories("missing-user", 3)).thenReturn(List.of());
        when(refreshService.generalBestsellers()).thenReturn(productIds(4));

        RecommendationResponse response = service.recommend("missing-user");

        assertThat(response.products()).isEmpty();
        assertThat(response.type()).isEqualTo(RecommendationType.NON_PERSONALIZED);
    }

    @Test
    void resultIsCappedAtTen() {
        stubPersonalized(rows(12));

        RecommendationResponse response = service.recommend("user-120");

        assertThat(response.products()).hasSize(10);
    }

    private void stubPersonalized(List<BestsellerRow> rows) {
        List<String> categories = List.of("category-a");
        when(categoryRepository.findTopCategories("user-120", 3)).thenReturn(categories);
        when(bestsellerRepository.findBestsellersForCategories(categories, 10)).thenReturn(rows);
    }

    private List<BestsellerRow> rows(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(index -> new BestsellerRow("product-" + index, count - index + 1))
                .toList();
    }

    private List<String> productIds(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(index -> "product-" + index)
                .toList();
    }
}
