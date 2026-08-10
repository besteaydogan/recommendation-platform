package com.besteaydogan.recoflow.history.api;

import java.util.List;

import com.besteaydogan.recoflow.history.application.BrowsingHistoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BrowsingHistoryController.class)
class BrowsingHistoryControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BrowsingHistoryService service;

    @Test
    void getReturnsExactAssignmentJson() throws Exception {
        when(service.getLatest("user-120")).thenReturn(new BrowsingHistoryResponse(
                "user-120",
                List.of("product-10", "product-10", "product-20"),
                "personalized"
        ));

        mockMvc.perform(get("/users/user-120/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['user-id']").value("user-120"))
                .andExpect(jsonPath("$.products[0]").value("product-10"))
                .andExpect(jsonPath("$.products[1]").value("product-10"))
                .andExpect(jsonPath("$.products[2]").value("product-20"))
                .andExpect(jsonPath("$.products.length()").value(3))
                .andExpect(jsonPath("$.type").value("personalized"))
                .andExpect(jsonPath("$.userId").doesNotExist());
    }

    @Test
    void getReturnsOkWithEmptyProducts() throws Exception {
        when(service.getLatest("missing-user")).thenReturn(new BrowsingHistoryResponse(
                "missing-user",
                List.of(),
                "personalized"
        ));

        mockMvc.perform(get("/users/missing-user/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['user-id']").value("missing-user"))
                .andExpect(jsonPath("$.products").isEmpty())
                .andExpect(jsonPath("$.type").value("personalized"));
    }

    @Test
    void deleteReturnsNoContentWhenRowsExist() throws Exception {
        when(service.deleteProduct("user-120", "product-10")).thenReturn(3);

        mockMvc.perform(delete("/users/user-120/history/product-10"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).deleteProduct("user-120", "product-10");
    }

    @Test
    void deleteReturnsNoContentWhenNothingMatches() throws Exception {
        when(service.deleteProduct("missing-user", "missing-product")).thenReturn(0);

        mockMvc.perform(delete("/users/missing-user/history/missing-product"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }
}
