package com.besteaydogan.recoflow.recommendation.api;

import java.util.List;

import com.besteaydogan.recoflow.recommendation.application.RecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecommendationController.class)
class RecommendationControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecommendationService service;

    @Test
    void returnsPersonalizedResponseWithExactJsonNames() throws Exception {
        when(service.recommend("user-120")).thenReturn(new RecommendationResponse(
                "user-120",
                List.of("product-1", "product-2", "product-3", "product-4", "product-5"),
                RecommendationType.PERSONALIZED
        ));

        mockMvc.perform(get("/users/user-120/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['user-id']").value("user-120"))
                .andExpect(jsonPath("$.products.length()").value(5))
                .andExpect(jsonPath("$.type").value("personalized"))
                .andExpect(jsonPath("$.userId").doesNotExist());
    }

    @Test
    void returnsNonPersonalizedResponse() throws Exception {
        when(service.recommend("missing-user")).thenReturn(new RecommendationResponse(
                "missing-user",
                List.of("product-1", "product-2", "product-3", "product-4", "product-5"),
                RecommendationType.NON_PERSONALIZED
        ));

        mockMvc.perform(get("/users/missing-user/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("non-personalized"));
    }

    @Test
    void returnsOkWithEmptyProducts() throws Exception {
        when(service.recommend("user-120")).thenReturn(new RecommendationResponse(
                "user-120",
                List.of(),
                RecommendationType.PERSONALIZED
        ));

        mockMvc.perform(get("/users/user-120/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products").isEmpty())
                .andExpect(jsonPath("$.type").value("personalized"));
    }
}
