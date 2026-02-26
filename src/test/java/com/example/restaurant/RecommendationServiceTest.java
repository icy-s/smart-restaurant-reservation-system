package com.example.restaurant;

import com.example.restaurant.model.MealSuggestion;
import com.example.restaurant.model.SearchResponse;
import com.example.restaurant.model.TableRecommendation;
import com.example.restaurant.model.Zone;
import com.example.restaurant.service.MealSuggestionService;
import com.example.restaurant.service.RecommendationService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecommendationServiceTest {

    private static final LocalDateTime FIXED_TIME = LocalDateTime.of(2026, 3, 10, 19, 0);
    private static final double MATCH_BONUS = 35.0;
    private static final double MISMATCH_PENALTY = -15.0;
    
    private final RecommendationService service;

    RecommendationServiceTest() {
        MealSuggestionService mealSuggestionService = mock(MealSuggestionService.class);
        when(mealSuggestionService.suggestMeal())
                .thenReturn(new MealSuggestion("Test Meal", "Test", "", "", true));
        this.service = new RecommendationService(mealSuggestionService);
    }
    
    @Test
    void shouldRecommendAtLeastOneTableForSmallParty() {
        SearchResponse response = service.recommend(FIXED_TIME, 2, null, false, false, false, false);

        assertTrue(response.tables().stream().anyMatch(t -> t.recommended() && !t.occupied()));
    }

    @Test
    void shouldProvideMergedRecommendationWhenSingleTableCannotFitParty() {
        SearchResponse response = service.recommend(FIXED_TIME, 11, Zone.INDOOR, false, false, false, false);

        assertTrue(response.tables().stream().anyMatch(t -> t.recommended() && t.merged()));
    }

    @Test
    void shouldApplyWindowPreferenceToTableScores() {
        assertPreferenceScoring(
                response -> response.table().window(),
                (dt) -> service.recommend(dt, 2, null, false, true, false, false)
        );
    }

    @Test
    void shouldApplyPrivacyPreferenceToTableScores() {
        assertPreferenceScoring(
                response -> response.table().privacy(),
                (dt) -> service.recommend(dt, 2, null, true, false, false, false)
        );
    }

    @Test
    void shouldApplyAccessibilityPreferenceToTableScores() {
        assertPreferenceScoring(
                response -> response.table().accessibility(),
                (dt) -> service.recommend(dt, 2, null, false, false, true, false)
        );
    }

    @Test
    void shouldApplyKidsAreaPreferenceToTableScores() {
        assertPreferenceScoring(
                response -> response.table().kidsArea(),
                (dt) -> service.recommend(dt, 2, null, false, false, false, true)
        );
    }

    @Test
    void shouldReturnMealSuggestionInResponse() {
        SearchResponse response = service.recommend(FIXED_TIME, 2, null, false, false, false, false);
        assertEquals("Test Meal", response.mealSuggestion().name());
    }

    private void assertPreferenceScoring(Predicate<TableRecommendation> hasFeature,
                                         ResponseProvider withPreferenceProvider) {
        SearchResponse withoutPreference = service.recommend(FIXED_TIME, 2,
                null, false, false, false, false);
        SearchResponse withPreference = withPreferenceProvider.get(FIXED_TIME);

        Map<String, TableRecommendation> baseById = toEligibleMap(withoutPreference);
        Map<String, TableRecommendation> withPrefById = toEligibleMap(withPreference);

        assertTrue(!baseById.isEmpty(), "Expected at least one free and eligible table");

        baseById.forEach((id, base) -> {
            TableRecommendation withPref = withPrefById.get(id);
            double expectedDelta = hasFeature.test(base) ? MATCH_BONUS : MISMATCH_PENALTY;
            assertEquals(base.score() + expectedDelta, withPref.score(), 0.001);
        });
    }

    private Map<String, TableRecommendation> toEligibleMap(SearchResponse response) {
        List<TableRecommendation> nonMerged = response.tables().stream()
            .filter(table -> !table.merged())
            .toList();

        return nonMerged.stream()
                .filter(table -> !table.occupied())
                .filter(table -> table.table().seats() >= 2)
                .collect(Collectors.toMap(table -> table.table().id(), table -> table));
    }

    @FunctionalInterface
    private interface ResponseProvider {
        SearchResponse get(LocalDateTime dateTime);
    }
}
