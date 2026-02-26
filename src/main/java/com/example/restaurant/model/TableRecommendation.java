package com.example.restaurant.model;

public record TableRecommendation(
        TableInfo table,
        boolean occupied,
        boolean recommended,
        double score,
        String reason
) {
}