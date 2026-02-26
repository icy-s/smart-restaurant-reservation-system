package com.example.restaurant.controller;

import com.example.restaurant.model.SearchResponse;
import com.example.restaurant.model.TableInfo;
import com.example.restaurant.model.TableLayoutUpdate;
import com.example.restaurant.model.Zone;
import com.example.restaurant.service.RecommendationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ReservationController {

    private final RecommendationService recommendationService;

    public ReservationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/availability")
    public SearchResponse availability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTime,
            @RequestParam(defaultValue = "2") int partySize,
            @RequestParam(required = false) Zone zone,
            @RequestParam(defaultValue = "false") boolean privacy,
            @RequestParam(defaultValue = "false") boolean window,
            @RequestParam(defaultValue = "false") boolean accessibility,
            @RequestParam(defaultValue = "false") boolean kidsArea
    ) {
        return recommendationService.recommend(dateTime, partySize, zone, privacy, window, accessibility, kidsArea);
    }

    @GetMapping("/admin/layout")
    public List<TableInfo> getLayout() {
        return recommendationService.currentTables();
    }

    @PutMapping("/admin/layout")
    public List<TableInfo> updateLayout(@RequestBody List<TableLayoutUpdate> updates) {
        return recommendationService.updateLayout(updates);
    }
}
