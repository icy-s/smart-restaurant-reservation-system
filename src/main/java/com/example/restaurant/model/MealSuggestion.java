package com.example.restaurant.model;

public record MealSuggestion(
        String name,
        String category,
        String thumbnail,
        String sourceUrl,
        boolean fallback
) {
}