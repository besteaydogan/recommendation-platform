package com.besteaydogan.recoflow.recommendation.application;

import java.util.List;

import com.besteaydogan.recoflow.common.observability.RecoFlowMetrics;
import com.besteaydogan.recoflow.history.infrastructure.TopCategoryQueryRepository;
import com.besteaydogan.recoflow.recommendation.api.RecommendationResponse;
import com.besteaydogan.recoflow.recommendation.api.RecommendationType;
import com.besteaydogan.recoflow.recommendation.infrastructure.BestsellerQueryRepository;
import com.besteaydogan.recoflow.recommendation.infrastructure.BestsellerRow;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {

    private static final int CATEGORY_LIMIT = 3;
    private static final int PRODUCT_LIMIT = 10;
    private static final int MINIMUM_PRODUCTS = 5;

    private final TopCategoryQueryRepository categoryRepository;
    private final BestsellerQueryRepository bestsellerRepository;
    private final BestsellerRefreshService refreshService;
    private final RecoFlowMetrics metrics;

    public RecommendationService(
            TopCategoryQueryRepository categoryRepository,
            BestsellerQueryRepository bestsellerRepository,
            BestsellerRefreshService refreshService,
            RecoFlowMetrics metrics
    ) {
        this.categoryRepository = categoryRepository;
        this.bestsellerRepository = bestsellerRepository;
        this.refreshService = refreshService;
        this.metrics = metrics;
    }

    public RecommendationResponse recommend(String userId) {
        return metrics.recordRecommendation(() -> calculateRecommendation(userId));
    }

    private RecommendationResponse calculateRecommendation(String userId) {
        List<String> categories = categoryRepository.findTopCategories(userId, CATEGORY_LIMIT);
        if (categories.isEmpty()) {
            return response(
                    userId,
                    refreshService.generalBestsellers(),
                    RecommendationType.NON_PERSONALIZED
            );
        }

        List<String> products = bestsellerRepository
                .findBestsellersForCategories(categories, PRODUCT_LIMIT)
                .stream()
                .map(BestsellerRow::productId)
                .toList();
        return response(userId, products, RecommendationType.PERSONALIZED);
    }

    private RecommendationResponse response(
            String userId,
            List<String> candidates,
            RecommendationType type
    ) {
        List<String> limited = candidates.stream()
                .limit(PRODUCT_LIMIT)
                .toList();
        List<String> products = limited.size() < MINIMUM_PRODUCTS ? List.of() : limited;
        return new RecommendationResponse(userId, products, type);
    }
}
