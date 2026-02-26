package com.example.restaurant;

import com.example.restaurant.model.SearchResponse;
import com.example.restaurant.model.TableRecommendation;
import com.example.restaurant.service.RecommendationService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationServiceTest {

    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2026, 3, 10, 19, 0);
    private final RecommendationService service = new RecommendationService();

    @Test
    void shouldRecommendAtLeastOneTableForSmallParty() {
        SearchResponse response = service.recommend(FIXED_TIME, 2, null, false, false, false, false);

        assertTrue(response.tables().stream().anyMatch(t -> t.recommended() && !t.occupied()));
    }

    @Test
    void shouldApplyWindowPreferenceToTableScores() {
        SearchResponse withoutWindow = service.recommend(FIXED_TIME, 2,
                null, false, false, false, false);
        SearchResponse withWindow = service.recommend(FIXED_TIME, 2,
                null, false, true, false, false);

        Map<String, TableRecommendation> baseById = toEligibleMap(withoutWindow, table -> table.table().window());
        Map<String, TableRecommendation> windowById = toEligibleMap(withWindow, table -> table.table().window());

        assertTrue(!baseById.isEmpty(), "Expected at least one free and eligible window table");

        baseById.forEach((id, base) -> {
            TableRecommendation withPref = windowById.get(id);
            assertEquals(base.score() + 20.0, withPref.score(), 0.001);
        });
    }

    @Test
    void shouldApplyPrivacyPreferenceToTableScores() {
        SearchResponse withoutPrivacy = service.recommend(FIXED_TIME, 2,
                null, false, false, false, false);
        SearchResponse withPrivacy = service.recommend(FIXED_TIME, 2,
                null, true, false, false, false);

        Map<String, TableRecommendation> baseById = toEligibleMap(withoutPrivacy, table -> table.table().privacy());
        Map<String, TableRecommendation> privacyById = toEligibleMap(withPrivacy, table -> table.table().privacy());

        assertTrue(!baseById.isEmpty(), "Expected at least one free and eligible privacy table");

        baseById.forEach((id, base) -> {
            TableRecommendation withPref = privacyById.get(id);
            assertEquals(base.score() + 20.0, withPref.score(), 0.001);
        });
    }

    private Map<String, TableRecommendation> toEligibleMap(SearchResponse response,
                                                           Predicate<TableRecommendation> featureEnabled) {
        return response.tables().stream()
                .filter(table -> !table.occupied())
                .filter(table -> table.table().seats() >= 2)
                .filter(featureEnabled)
                .collect(Collectors.toMap(table -> table.table().id(), table -> table));
    }
}
