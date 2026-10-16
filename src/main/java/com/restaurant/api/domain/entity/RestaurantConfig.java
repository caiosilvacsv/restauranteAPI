package com.restaurant.api.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZoneId;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "restaurant_config")
public class RestaurantConfig {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "restaurant_name", nullable = false)
  private String restaurantName = "La Bella Pizzaria";

  @Column(name = "whatsapp_phone", nullable = false)
  private String whatsappPhone = "5538991570684";

  @Column(name = "pix_key")
  private String pixKey = "38991570684";

  @Column(name = "type_pix_key")
  private String typePixKey = "TELEFONE";

  @Column(name = "delivery_fee", nullable = false)
  private BigDecimal deliveryFee = BigDecimal.valueOf(2.00);

  private String address = "Taiobeiras - MG";

  @Column(name = "open_time")
  private LocalTime openTime = LocalTime.of(18, 0);

  @Column(name = "close_time")
  private LocalTime closeTime = LocalTime.of(23, 30);

  @Column(name = "open_days", length = 120)
  private String openDays = "TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY";

  @Column(name = "is_manually_closed", nullable = false)
  private boolean isManuallyClosed = false;

  @Column(name = "closed_message")
  private String closedMessage = "Estamos fechados no momento. Não estamos recebendo pedidos!";

  @Column(name = "estimated_time_minutes")
  private Integer estimatedTimeMinutes = 40;

  @Column(name = "estimated_time_text", length = 50)
  private String estimatedTimeText = "40 - 50 minutos";

  public void updateEstimate(Integer minutes) {
    this.estimatedTimeMinutes = minutes;
    if (minutes != null) {
      this.estimatedTimeText = minutes + " - " + (minutes + 10) + " minutos";
    }
  }

  public java.util.List<String> getOpenDaysList() {
    if (this.openDays == null || this.openDays.isBlank()) {
      return java.util.List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY");
    }
    return java.util.Arrays.stream(this.openDays.split(","))
        .map(String::trim)
        .map(String::toUpperCase)
        .filter(s -> !s.isEmpty())
        .toList();
  }

  public void setOpenDaysList(java.util.List<String> days) {
    if (days == null || days.isEmpty()) {
      this.openDays = "TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY";
    } else {
      this.openDays = String.join(",", days.stream().map(String::trim).map(String::toUpperCase).toList());
    }
  }

  public boolean isOpenOn(java.time.DayOfWeek day) {
    if (day == null) return false;
    return getOpenDaysList().contains(day.name());
  }

  public boolean isCurrentlyOpen() {
    if (this.isManuallyClosed) {
      return false;
    }
    if (this.openTime == null || this.closeTime == null) {
      return true;
    }
    ZoneId zone = ZoneId.of("America/Sao_Paulo");
    LocalTime nowTime = LocalTime.now(zone);
    java.time.LocalDate today = java.time.LocalDate.now(zone);

    // Se o expediente cruza meia-noite (ex: 18:00 às 02:00) e agora são 01:00,
    // o dia de expediente de referência é o de ontem.
    java.time.DayOfWeek businessDay = today.getDayOfWeek();
    if (closeTime.isBefore(openTime) && nowTime.isBefore(closeTime)) {
      businessDay = today.minusDays(1).getDayOfWeek();
    }

    if (!isOpenOn(businessDay)) {
      return false;
    }

    if (closeTime.isAfter(openTime)) {
      return !nowTime.isBefore(openTime) && !nowTime.isAfter(closeTime);
    } else {
      // Cruza meia-noite (ex: 18:00 às 01:00)
      return !nowTime.isBefore(openTime) || !nowTime.isAfter(closeTime);
    }
  }
}
