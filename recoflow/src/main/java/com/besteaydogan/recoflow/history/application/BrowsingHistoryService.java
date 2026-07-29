package com.besteaydogan.recoflow.history.application;

import java.util.List;

import com.besteaydogan.recoflow.history.api.BrowsingHistoryResponse;
import com.besteaydogan.recoflow.history.infrastructure.ProductViewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BrowsingHistoryService {

    private static final String PERSONALIZED = "personalized";

    private final ProductViewRepository repository;

    public BrowsingHistoryService(ProductViewRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public BrowsingHistoryResponse getLatest(String userId) {
        List<String> productIds = repository.findTop10ByUserIdOrderByViewedAtDescIdDesc(userId)
                .stream()
                .map(productView -> productView.getProductId())
                .toList();
        return new BrowsingHistoryResponse(userId, productIds, PERSONALIZED);
    }

    @Transactional
    public int deleteProduct(String userId, String productId) {
        return repository.deleteAllByUserIdAndProductId(userId, productId);
    }
}
