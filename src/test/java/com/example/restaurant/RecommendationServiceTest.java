package com.example.restaurant;

import com.example.restaurant.model.SearchResponse;
import com.example.restaurant.service.RecommendationService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationServiceTest {

    private final RecommendationService service = new RecommendationService();

    @Test
    void shouldRecommendAtLeastOneTableForSmallParty() {
        SearchResponse response = service.recommend(LocalDateTime.of(2026, 3, 10, 19, 0), 2,
                null, false, false, false, false);

        assertTrue(response.tables().stream().anyMatch(t -> t.recommended() && !t.occupied()));
    }
}
