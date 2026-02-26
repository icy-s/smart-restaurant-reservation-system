package com.example.restaurant.service;

import com.example.restaurant.model.MealSuggestion;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class MealSuggestionService {

    private static final String RANDOM_MEAL_URL = "https://www.themealdb.com/api/json/v1/1/random.php";

    private final RestTemplate restTemplate;

    public MealSuggestionService(RestTemplateBuilder builder) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(2))
                .build();
    }

    public MealSuggestion suggestMeal() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(RANDOM_MEAL_URL, Map.class);
            Object mealsObject = response.getBody() == null ? null : response.getBody().get("meals");
            if (!(mealsObject instanceof List<?> meals) || meals.isEmpty()) {
                return fallback();
            }

            Object firstMeal = meals.getFirst();
            if (!(firstMeal instanceof Map<?, ?> meal)) {
                return fallback();
            }

            return new MealSuggestion(
                    stringOrFallback(meal.get("strMeal"), "Päevapraad"),
                    stringOrFallback(meal.get("strCategory"), "Määramata"),
                    stringOrFallback(meal.get("strMealThumb"), ""),
                    stringOrFallback(meal.get("strSource"), "https://www.themealdb.com"),
                    false
            );
        } catch (Exception ex) {
            return fallback();
        }
    }

    private MealSuggestion fallback() {
        return new MealSuggestion(
                "Koka üllatusroog",
                "Majasoovitus",
                "",
                "https://www.themealdb.com",
                true
        );
    }

    private String stringOrFallback(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? fallback : text;
    }
}
