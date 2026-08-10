package com.besteaydogan.recoflow.common.exception;

import java.net.URI;

import com.besteaydogan.recoflow.history.api.BrowsingHistoryController;
import com.besteaydogan.recoflow.history.application.BrowsingHistoryService;
import com.besteaydogan.recoflow.recommendation.api.RecommendationController;
import com.besteaydogan.recoflow.recommendation.application.RecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({BrowsingHistoryController.class, RecommendationController.class})
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BrowsingHistoryService historyService;

    @MockitoBean
    private RecommendationService recommendationService;

    @Test
    void blankLikePathVariableReturnsBadRequest() throws Exception {
        mockMvc.perform(get(URI.create("/users/%20/history")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("userId must not be blank"))
                .andExpect(jsonPath("$.path").value("/users/%20/history"))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());

        verifyNoInteractions(historyService);
    }

    @Test
    void unexpectedServiceFailureReturnsSafeInternalServerError() throws Exception {
        when(recommendationService.recommend("user-120"))
                .thenThrow(new IllegalStateException("SQL password=secret"));

        mockMvc.perform(get("/users/user-120/recommendations"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected server error occurred"))
                .andExpect(jsonPath("$.path").value("/users/user-120/recommendations"))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(content().string(not(containsString("SQL password=secret"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }
}
