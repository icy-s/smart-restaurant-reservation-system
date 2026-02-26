package com.example.restaurant.model;

import java.util.List;

public record TableRecommendation(
        TableInfo table,
        boolean occupied,
        boolean recommended,
        double score,
        String reason,
        boolean merged,
        List<String> mergedTableIds
) {
}