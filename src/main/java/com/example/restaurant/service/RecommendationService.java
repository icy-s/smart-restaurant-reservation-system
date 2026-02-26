package com.example.restaurant.service;

import com.example.restaurant.model.MealSuggestion;
import com.example.restaurant.model.SearchRequest;
import com.example.restaurant.model.SearchResponse;
import com.example.restaurant.model.TableInfo;
import com.example.restaurant.model.TableLayoutUpdate;
import com.example.restaurant.model.TableRecommendation;
import com.example.restaurant.model.Zone;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private static final double ADJACENCY_THRESHOLD = 190;

    private static final List<TableInfo> BASE_TABLES = List.of(
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

    private final MealSuggestionService mealSuggestionService;
    private final Map<String, TableInfo> tableLayout = new ConcurrentHashMap<>();

    public RecommendationService(MealSuggestionService mealSuggestionService) {
        this.mealSuggestionService = mealSuggestionService;
        BASE_TABLES.forEach(table -> tableLayout.put(table.id(), table));
    }

    public SearchResponse recommend(LocalDateTime dateTime, int partySize, Zone zone,
                                    boolean privacy, boolean window, boolean accessibility, boolean kidsArea) {
        SearchRequest request = new SearchRequest(dateTime, partySize, zone, privacy, window, accessibility, kidsArea);
        Set<String> occupiedIds = generateOccupiedTables(dateTime);
        List<TableInfo> currentTables = currentTables();

        List<TableRecommendation> filtered = currentTables.stream()
                .filter(table -> zone == null || table.zone() == zone)
                .map(table -> toRecommendation(table, request, occupiedIds.contains(table.id())))
                .sorted(Comparator.comparing(TableRecommendation::occupied)
                        .thenComparing(TableRecommendation::score, Comparator.reverseOrder()))
                .collect(Collectors.toCollection(ArrayList::new));

        TableRecommendation bestSingle = filtered.stream()
                .filter(t -> !t.occupied())
                .filter(t -> t.table().seats() >= partySize)
                .max(Comparator.comparing(TableRecommendation::score))
                .orElse(null);

        boolean hasExactCapacityTable = hasExactCapacityTable(currentTables, zone, partySize);

        TableRecommendation mergedOption = null;
        if (partySize > 1 && bestSingle == null && !hasExactCapacityTable) {
            mergedOption = findMergedOption(request, currentTables, occupiedIds);
            if (mergedOption != null) {
                filtered.add(0, mergedOption);
            }
        }

        String bestId = bestSingle == null ? null : bestSingle.table().id();
        List<String> mergedIds = mergedOption == null ? List.of() : mergedOption.mergedTableIds();

        List<TableRecommendation> marked = filtered.stream()
                .map(t -> new TableRecommendation(
                        t.table(),
                        t.occupied(),
                        (bestId != null && t.table().id().equals(bestId)) || (!mergedIds.isEmpty() && t.merged() && t.mergedTableIds().equals(mergedIds)),
                        t.score(),
                        t.reason(),
                        t.merged(),
                        t.mergedTableIds()
                ))
                .toList();

        MealSuggestion mealSuggestion = mealSuggestionService.suggestMeal();
        String info = "Skoor = sobivus seltskonna suurusega + eelistuste boonused/trahvid. " +
                "Kui ühte lauda ei leidu, kontrollitakse kõrvuti laudu, mida saab kokku lükata. " +
                "Hõive arvutuses eeldatakse, et külastus kestab ~2-3 tundi.";
        return new SearchResponse(request, marked, info, mealSuggestion);
    }

    public List<TableInfo> currentTables() {
        return BASE_TABLES.stream()
                .map(table -> tableLayout.getOrDefault(table.id(), table))
                .toList();
    }

    public List<TableInfo> updateLayout(List<TableLayoutUpdate> updates) {
        Map<String, TableLayoutUpdate> updateMap = new HashMap<>();
        updates.forEach(update -> updateMap.put(update.id(), update));

        for (TableInfo table : BASE_TABLES) {
            TableLayoutUpdate update = updateMap.get(table.id());
            if (update == null) {
                continue;
            }
            tableLayout.put(table.id(), new TableInfo(
                    table.id(), table.seats(), table.zone(),
                    update.x(), update.y(),
                    table.privacy(), table.window(), table.accessibility(), table.kidsArea()
            ));
        }
        return currentTables();
    }

    private boolean hasExactCapacityTable(List<TableInfo> tables, Zone zone, int partySize) {
        return tables.stream()
                .filter(table -> zone == null || table.zone() == zone)
                .anyMatch(table -> table.seats() == partySize);
    }

    private TableRecommendation findMergedOption(SearchRequest request, List<TableInfo> tables, Set<String> occupiedIds) {
        List<TableInfo> availableTables = tables.stream()
                .filter(table -> !occupiedIds.contains(table.id()))
                .filter(table -> request.zone() == null || table.zone() == request.zone())
                .toList();

        if (availableTables.size() < 2) {
            return null;
        }

        TableRecommendation bestOption = null;
        int maxGroupSize = Math.min(4, availableTables.size());
        for (int groupSize = 2; groupSize <= maxGroupSize; groupSize++) {
            bestOption = findBestMergedCombination(request, availableTables, groupSize, bestOption);
        }
        return bestOption;
    }

        private TableRecommendation findBestMergedCombination(SearchRequest request,
                                                          List<TableInfo> availableTables,
                                                          int groupSize,
                                                          TableRecommendation currentBest) {
        int[] indexes = new int[groupSize];
        for (int i = 0; i < groupSize; i++) {
            indexes[i] = i;
        }

        TableRecommendation best = currentBest;
        while (true) {
            List<TableInfo> combination = new ArrayList<>(groupSize);
            for (int index : indexes) {
                combination.add(availableTables.get(index));
            }

            if (isConnectedGroup(combination)) {
                int totalSeats = combination.stream().mapToInt(TableInfo::seats).sum();
                if (totalSeats >= request.partySize()) {
                    TableInfo merged = mergeTables(combination);
                    TableRecommendation recommendation = toRecommendation(merged, request, false);

                    List<String> ids = combination.stream().map(TableInfo::id).toList();
                    recommendation = new TableRecommendation(
                            recommendation.table(),
                            false,
                            false,
                            recommendation.score() + (8 * (groupSize - 1)),
                            recommendation.reason() + " · Kombineeritud lauad: " + String.join(" + ", ids),
                            true,
                            ids
                    );

                    if (best == null || recommendation.score() > best.score()) {
                        best = recommendation;
                    }
                }
            }

                int next = groupSize - 1;
            while (next >= 0 && indexes[next] == availableTables.size() - groupSize + next) {
                next--;
            }
            if (next < 0) {
                break;
            }

            indexes[next]++;
            for (int i = next + 1; i < groupSize; i++) {
                indexes[i] = indexes[i - 1] + 1;
            }
        }
        return best;
    }

    private boolean isConnectedGroup(List<TableInfo> tables) {
        Set<String> visited = new LinkedHashSet<>();
        visited.add(tables.get(0).id());

        boolean changed = true;
        while (changed) {
            changed = false;
            for (TableInfo table : tables) {
                if (visited.contains(table.id())) {
                    continue;
                }

                boolean touchesVisited = tables.stream()
                        .filter(candidate -> visited.contains(candidate.id()))
                        .anyMatch(candidate -> isAdjacent(candidate, table));
                if (touchesVisited) {
                    visited.add(table.id());
                    changed = true;
                }
            }
        }
        return visited.size() == tables.size();
    }

    private boolean isAdjacent(TableInfo first, TableInfo second) {
        double distance = Math.hypot(first.x() - second.x(), first.y() - second.y());
        return distance <= ADJACENCY_THRESHOLD;
    }

     private TableInfo mergeTables(List<TableInfo> tables) {
        int totalSeats = tables.stream().mapToInt(TableInfo::seats).sum();
        int avgX = (int) Math.round(tables.stream().mapToInt(TableInfo::x).average().orElse(0));
        int avgY = (int) Math.round(tables.stream().mapToInt(TableInfo::y).average().orElse(0));
        return new TableInfo(
                tables.stream().map(TableInfo::id).collect(Collectors.joining("+")),
                totalSeats,
                tables.get(0).zone(),
                avgX,
                avgY,
                tables.stream().anyMatch(TableInfo::privacy),
                tables.stream().anyMatch(TableInfo::window),
                tables.stream().anyMatch(TableInfo::accessibility),
                tables.stream().anyMatch(TableInfo::kidsArea)
        );
    }

    private TableRecommendation toRecommendation(TableInfo table, SearchRequest request, boolean occupied) {
        if (occupied) {
            return new TableRecommendation(table, true, false, -1, "Hõivatud valitud ajal", false, List.of());
        }
        if (table.seats() < request.partySize()) {
            return new TableRecommendation(table, false, false, -1, "Liiga väike laud", false, List.of());
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
        return new TableRecommendation(table, false, false, score, reason, false, List.of());
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
        Set<String> occupied = new HashSet<>();
        LocalDate date = dateTime.toLocalDate();

    for (TableInfo table : currentTables()) {
            Random random = new Random((long) date.toEpochDay() * 31 + table.id().hashCode());
            LocalDateTime cursor = date.atTime(12, 0).plusMinutes(random.nextInt(40));
            int reservationCount = 1 + random.nextInt(4);

            for (int i = 0; i < reservationCount; i++) {
                int stayMinutes = 120 + random.nextInt(61);
                LocalDateTime end = cursor.plusMinutes(stayMinutes);
                if (!dateTime.isBefore(cursor) && dateTime.isBefore(end)) {
                    occupied.add(table.id());
                    break;
                }

                int gapMinutes = 25 + random.nextInt(70);
                cursor = end.plusMinutes(gapMinutes);
                if (cursor.getHour() >= 23) {
                    break;
                }
            }
        }

        return occupied;
    }
}
