package com.example.restaurant.model;

import java.time.LocalDateTime;

public record SearchRequest(
        LocalDateTime dateTime,
        int partySize,
        Zone zone,
        boolean privacy,
        boolean window,
        boolean accessibility,
        boolean kidsArea
) {
}