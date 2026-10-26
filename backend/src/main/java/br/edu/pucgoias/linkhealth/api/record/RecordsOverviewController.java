package br.edu.pucgoias.linkhealth.api.record;

import br.edu.pucgoias.linkhealth.service.PatientPlannedMealsService;
import br.edu.pucgoias.linkhealth.service.RecordsOverviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/patients/{patientId}")
@Tag(name = "Resumo e histórico dos registros")
public class RecordsOverviewController {
    private final RecordsOverviewService overview;
    private final PatientPlannedMealsService plannedMeals;
    public RecordsOverviewController(RecordsOverviewService overview, PatientPlannedMealsService plannedMeals) {
        this.overview = overview;
        this.plannedMeals = plannedMeals;
    }

    @GetMapping("/records-summary")
    @Operation(summary = "Consulta o resumo diário e o contexto do acompanhamento ativo mais recente")
    public RecordsSummaryResponse summary(@PathVariable UUID patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "America/Sao_Paulo") String zoneId) {
        return overview.summary(patientId, date, zoneId);
    }

    @GetMapping("/records-history")
    @Operation(summary = "Agrupa os registros reais por dia no fuso horário informado")
    public List<RecordsHistoryResponse> history(@PathVariable UUID patientId,
            @RequestParam(defaultValue = "America/Sao_Paulo") String zoneId) {
        return overview.history(patientId, zoneId);
    }

    @GetMapping("/planned-meals")
    @Operation(summary = "Consulta refeições dos planos ativos para registrar sua realização")
    public List<PlannedMealResponse> meals(@PathVariable UUID patientId) {
        return plannedMeals.list(patientId).stream().map(PlannedMealResponse::from).toList();
    }
}
