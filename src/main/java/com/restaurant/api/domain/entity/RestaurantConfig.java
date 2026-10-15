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

  @Column(name = "is_manually_closed", nullable = false)
  private boolean isManuallyClosed = false;

  @Column(name = "closed_message")
  private String closedMessage = "Estamos fechados no momento. Não estamos recebendo pedidos!";

  @Column(name = "estimated_time_minutes")
  private Integer estimatedTimeMinutes = 40;

  @Column(name = "estimated_time_text", length = 50)
  private String estimatedTimeText = "40-50 min";

  public boolean isCurrentlyOpen() {
    if (this.isManuallyClosed) {
      return false;
    }
    if (this.openTime == null || this.closeTime == null) {
      return true;
    }
    LocalTime now = LocalTime.now(ZoneId.of("America/Sao_Paulo"));
    if (closeTime.isAfter(openTime)) {
      return !now.isBefore(openTime) && !now.isAfter(closeTime);
    } else {
      // Cruza meia-noite (ex: 18:00 às 01:00)
      return !now.isBefore(openTime) || !now.isAfter(closeTime);
    }
  }
}
