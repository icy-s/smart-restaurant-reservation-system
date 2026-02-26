package com.example.restaurant.service;

import com.example.restaurant.model.SearchRequest;
import com.example.restaurant.model.SearchResponse;
import com.example.restaurant.model.TableInfo;
import com.example.restaurant.model.TableRecommendation;
import com.example.restaurant.model.Zone;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private static final List<TableInfo> TABLES = List.of(
            new TableInfo("T1", 2, Zone.INDOOR, 80, 90, true, true, true, false),
            new TableInfo("T2", 2, Zone.INDOOR, 200, 90, false, false, true, true),
            new TableInfo("T3", 4, Zone.INDOOR, 330, 90, false, true, true, false),
            new TableInfo("T4", 4, Zone.INDOOR, 470, 90, true, false, false, false),
            new TableInfo("T5", 6, Zone.INDOOR, 600, 90, false, false, true, true),
            new TableInfo("T6", 2, Zone.TERRACE, 100, 240, true, true, false, false),
            new TableInfo("T7", 4, Zone.TERRACE, 250, 240, false, true, false, false),
            new TableInfo("T8", 6, Zone.TERRACE, 420, 240, false, true, false, true),
            new TableInfo("T9", 8, Zone.PRIVATE_ROOM, 590, 240, true, false, true, false),
            new TableInfo("T10", 10, Zone.PRIVATE_ROOM, 360, 380, true, false, true, true)
    );

    public SearchResponse recommend(LocalDateTime dateTime, int partySize, Zone zone,
                                    boolean privacy, boolean window, boolean accessibility, boolean kidsArea) {
        SearchRequest request = new SearchRequest(dateTime, partySize, zone, privacy, window, accessibility, kidsArea);
        Set<String> occupiedIds = generateOccupiedTables(dateTime);

        List<TableRecommendation> filtered = TABLES.stream()
                .filter(table -> zone == null || table.zone() == zone)
                .map(table -> toRecommendation(table, request, occupiedIds.contains(table.id())))
                .sorted(Comparator.comparing(TableRecommendation::occupied)
                        .thenComparing(TableRecommendation::score, Comparator.reverseOrder()))
                .toList();

        String bestId = filtered.stream()
                .filter(t -> !t.occupied())
                .filter(t -> t.table().seats() >= partySize)
                .max(Comparator.comparing(TableRecommendation::score))
                .map(t -> t.table().id())
                .orElse(null);

        List<TableRecommendation> marked = filtered.stream()
                .map(t -> new TableRecommendation(t.table(), t.occupied(), t.table().id().equals(bestId), t.score(), t.reason()))
                .collect(Collectors.toList());

        String info = "Skoor = sobivus seltskonna suurusega + eelistuste boonused ja trahvid. " +
                "Kui eelistus on valitud, siis sobiv laud saab +35 punkti, mittesobiv -15 punkti.";
        return new SearchResponse(request, marked, info);
    }

    private TableRecommendation toRecommendation(TableInfo table, SearchRequest request, boolean occupied) {
        if (occupied) {
            return new TableRecommendation(table, true, false, -1, "Hõivatud valitud ajal");
        }
        if (table.seats() < request.partySize()) {
            return new TableRecommendation(table, false, false, -1, "Liiga väike laud");
        }

        int emptySeats = table.seats() - request.partySize();
        double score = 100 - (emptySeats * 12.0);
        List<String> preferenceDetails = new ArrayList<>();

        score += applyPreferenceBoostOrPenalty(request.privacy(), table.privacy(), preferenceDetails, "privaatsus");
        score += applyPreferenceBoostOrPenalty(request.window(), table.window(), preferenceDetails, "akna all");
        score += applyPreferenceBoostOrPenalty(request.accessibility(), table.accessibility(), preferenceDetails, "ligipääsetav");
        score += applyPreferenceBoostOrPenalty(request.kidsArea(), table.kidsArea(), preferenceDetails, "lasteala lähedal");

        if (request.zone() != null && table.zone() == request.zone()) {
            score += 10;
            preferenceDetails.add("tsoon sobib");
        }

        String reason = "Sobib " + request.partySize() + " külalisele; vabu toole " + emptySeats;
        if (!preferenceDetails.isEmpty()) {
            reason += " · " + String.join(", ", preferenceDetails);
        }
        return new TableRecommendation(table, false, false, score, reason);
    }

    private double applyPreferenceBoostOrPenalty(boolean requested, boolean supported,
                                                 List<String> details, String label) {
        if (!requested) {
            return 0;
        }
        if (supported) {
            details.add(label + " ✓");
            return 35;
        }
        details.add(label + " ✕");
        return -15;
    }

    private Set<String> generateOccupiedTables(LocalDateTime dateTime) {
        long seed = dateTime.getYear() * 1_000_000L + dateTime.getDayOfYear() * 10_000L + dateTime.getHour() * 100L + dateTime.getMinute();
        Random random = new Random(seed);

        return TABLES.stream()
                .filter(table -> random.nextDouble() < occupancyProbability(table.zone()))
                .map(TableInfo::id)
                .collect(Collectors.toSet());
    }

    private double occupancyProbability(Zone zone) {
        return switch (zone) {
            case TERRACE -> 0.35;
            case PRIVATE_ROOM -> 0.45;
            case INDOOR -> 0.4;
        };
    }
}
