package br.edu.pucgoias.linkhealth.service;

import br.edu.pucgoias.linkhealth.api.record.RecordsHistoryResponse;
import br.edu.pucgoias.linkhealth.api.record.RecordsSummaryResponse;
import br.edu.pucgoias.linkhealth.domain.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecordsOverviewService {
    private final WaterIntakeRepository water;
    private final MealRecordRepository meals;
    private final WeightRecordRepository weights;
    private final EvolutionPhotoRecordRepository photos;
    private final FollowUpRepository followUps;
    private final UserAccountService accounts;
    private final PatientAccessService access;
    private final PatientPlannedMealsService plans;

    public RecordsOverviewService(WaterIntakeRepository water, MealRecordRepository meals,
            WeightRecordRepository weights, EvolutionPhotoRecordRepository photos, FollowUpRepository followUps,
            UserAccountService accounts, PatientAccessService access, PatientPlannedMealsService plans) {
        this.water = water;
        this.meals = meals;
        this.weights = weights;
        this.photos = photos;
        this.followUps = followUps;
        this.accounts = accounts;
        this.access = access;
        this.plans = plans;
    }

    @Transactional(readOnly = true)
    public RecordsSummaryResponse summary(UUID patientId, LocalDate requestedDate, String zoneId) {
        authorize(patientId);
        ZoneId zone = RecordDates.zone(zoneId);
        LocalDate date = requestedDate == null ? LocalDate.now(zone) : requestedDate;
        FollowUp current = followUps.findByPatientId(patientId).stream()
                .filter(f -> f.status() == FollowUpStatus.ATIVO)
                .max(Comparator.comparing(FollowUp::invitedAt).thenComparing(FollowUp::id)).orElse(null);
        BigDecimal weight = weights.findByPatientId(patientId).stream()
                .filter(w -> !w.measuredAt().atZone(zone).toLocalDate().isAfter(date))
                .max(Comparator.comparing(WeightRecord::measuredAt).thenComparing(WeightRecord::id))
                .map(WeightRecord::weightKg).orElse(null);
        return new RecordsSummaryResponse(date,
                water.findByPatientId(patientId).stream().filter(w -> on(w.consumedAt(), date, zone))
                        .mapToLong(WaterIntake::amountMilliliters).sum(),
                current == null ? null : current.waterGoalMl(),
                current == null ? null : current.objective(),
                current == null ? null : accounts.find(current.nutritionistId()).fullName(),
                meals.findByPatientId(patientId).stream().filter(m -> on(m.consumedAt(), date, zone)).count(),
                plans.list(patientId).size(), weight,
                photos.findByPatientId(patientId).stream()
                        .filter(p -> p.uploadStatus() == PhotoUploadStatus.STORED && on(p.capturedAt(), date, zone)).count());
    }

    @Transactional(readOnly = true)
    public List<RecordsHistoryResponse> history(UUID patientId, String zoneId) {
        authorize(patientId);
        ZoneId zone = RecordDates.zone(zoneId);
        Map<LocalDate, Day> days = new TreeMap<>(Comparator.reverseOrder());
        for (WaterIntake w : water.findByPatientId(patientId)) {
            day(days, w.consumedAt(), zone).water += w.amountMilliliters();
        }
        for (MealRecord m : meals.findByPatientId(patientId)) day(days, m.consumedAt(), zone).meals++;
        for (WeightRecord w : weights.findByPatientId(patientId)) {
            Day entry = day(days, w.measuredAt(), zone);
            if (entry.weightAt == null || w.measuredAt().isAfter(entry.weightAt)
                    || (w.measuredAt().equals(entry.weightAt) && w.id().compareTo(entry.weightId) > 0)) {
                entry.weightAt = w.measuredAt();
                entry.weightId = w.id();
                entry.weight = w.weightKg();
            }
        }
        for (EvolutionPhotoRecord p : photos.findByPatientId(patientId)) {
            if (p.uploadStatus() == PhotoUploadStatus.STORED) day(days, p.capturedAt(), zone).photos++;
        }
        return days.entrySet().stream().map(e -> new RecordsHistoryResponse(e.getKey(), e.getValue().water,
                e.getValue().meals, e.getValue().weight, e.getValue().photos)).toList();
    }

    private void authorize(UUID patientId) {
        access.ensureCanAccess(patientId);
        accounts.requirePatient(patientId);
    }
    private boolean on(Instant value, LocalDate day, ZoneId zone) {
        return value.atZone(zone).toLocalDate().equals(day);
    }
    private Day day(Map<LocalDate, Day> days, Instant instant, ZoneId zone) {
        return days.computeIfAbsent(instant.atZone(zone).toLocalDate(), ignored -> new Day());
    }
    private static class Day {
        long water;
        long meals;
        long photos;
        BigDecimal weight;
        Instant weightAt;
        UUID weightId;
    }
}
