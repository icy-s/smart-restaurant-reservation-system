package com.example.restaurant.model;

import java.util.List;

public record SearchResponse(
        SearchRequest request,
        List<TableRecommendation> tables,
        String algorithmInfo
) {
}