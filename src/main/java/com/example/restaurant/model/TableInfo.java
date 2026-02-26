package com.example.restaurant.model;

public record TableInfo(
        String id,
        int seats,
        Zone zone,
        int x,
        int y,
        boolean privacy,
        boolean window,
        boolean accessibility,
        boolean kidsArea
) {
}