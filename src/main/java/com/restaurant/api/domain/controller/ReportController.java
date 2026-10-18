package com.restaurant.api.domain.controller;

import com.restaurant.api.domain.dto.report.DailyReportResponse;
import com.restaurant.api.domain.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/reports")
@Tag(name = "Relatórios e Fechamento de Caixa", description = "Consolidação oficial de faturamento, conferência de caixa e métricas operacionais")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

  private final ReportService reportService;

  public ReportController(ReportService reportService) {
    this.reportService = reportService;
  }

  @GetMapping("/daily")
  @Operation(
      summary = "Obter relatório diário consolidado (Fechamento de Caixa)",
      description = "Retorna o faturamento bruto, líquido, divisão por forma de pagamento (Dinheiro, PIX, Cartão), canais (Mesa vs Delivery), métricas de pedidos e produtos mais vendidos. Se a data não for informada, assume o expediente atual considerando a virada de meia-noite."
  )
  public ResponseEntity<DailyReportResponse> getDailyReport(
      @Parameter(description = "Data do expediente no formato AAAA-MM-DD (ex: 2026-10-17). Opcional, padrão é a data operacional atual.")
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
      LocalDate date
  ) {
    DailyReportResponse report = reportService.getDailyReport(date);
    return ResponseEntity.ok(report);
  }
}
