package com.besteaydogan.recoflow.recommendation.api;

import com.besteaydogan.recoflow.recommendation.application.RecommendationService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/{userId}/recommendations")
public class RecommendationController {

    private final RecommendationService service;

    public RecommendationController(RecommendationService service) {
        this.service = service;
    }

    @GetMapping
    public RecommendationResponse recommendations(
            @PathVariable("userId") @NotBlank(message = "userId must not be blank") String userId
    ) {
        return service.recommend(userId);
    }
}
