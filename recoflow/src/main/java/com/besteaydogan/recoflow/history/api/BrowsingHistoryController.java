package com.besteaydogan.recoflow.history.api;

import com.besteaydogan.recoflow.history.application.BrowsingHistoryService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/{userId}/history")
public class BrowsingHistoryController {

    private final BrowsingHistoryService service;

    public BrowsingHistoryController(BrowsingHistoryService service) {
        this.service = service;
    }

    @GetMapping
    public BrowsingHistoryResponse getHistory(
            @PathVariable("userId") @NotBlank(message = "userId must not be blank") String userId
    ) {
        return service.getLatest(userId);
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(
            @PathVariable("userId") @NotBlank(message = "userId must not be blank") String userId,
            @PathVariable("productId") @NotBlank(message = "productId must not be blank") String productId
    ) {
        service.deleteProduct(userId, productId);
    }
}
