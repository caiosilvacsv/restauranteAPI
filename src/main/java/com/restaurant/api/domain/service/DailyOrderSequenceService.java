package com.restaurant.api.domain.service;

import com.restaurant.api.domain.entity.RestaurantConfig;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
public class DailyOrderSequenceService {

  public static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Sao_Paulo");

  private final JdbcTemplate jdbcTemplate;

  public DailyOrderSequenceService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /**
   * Calcula a data operacional do expediente comercial.
   * Se o expediente cruzar a meia-noite (ex: 18:00 às 02:00) e a hora atual estiver entre 00:00 e o fechamento,
   * a data de negócio é o dia anterior, mantendo todos os pedidos daquela noite no mesmo expediente.
   */
  public LocalDate getBusinessDate(RestaurantConfig config) {
    ZonedDateTime now = ZonedDateTime.now(DEFAULT_ZONE);
    return calculateBusinessDate(now, config != null ? config.getOpenTime() : null, config != null ? config.getCloseTime() : null);
  }

  /**
   * Lógica pura de determinação da data do expediente (fácil de testar isoladamente).
   */
  public LocalDate calculateBusinessDate(ZonedDateTime dateTime, LocalTime openTime, LocalTime closeTime) {
    LocalDate date = dateTime.toLocalDate();
    LocalTime time = dateTime.toLocalTime();

    if (openTime != null && closeTime != null) {
      // Expediente cruza a meia-noite (ex: 18:00 às 02:00)
      if (closeTime.isBefore(openTime)) {
        // Se ainda não passou do horário de fechamento na madrugada (ex: 01:30 <= 02:00)
        if (time.isBefore(closeTime) || time.equals(closeTime)) {
          return date.minusDays(1);
        }
      }
    }
    return date;
  }

  /**
   * Obtém o próximo sequencial diário atomicamente no PostgreSQL.
   */
  @Transactional
  public int nextSequence(LocalDate businessDate) {
    String sql = """
        INSERT INTO daily_order_sequence (business_date, last_sequence, updated_at)
        VALUES (?, 1, CURRENT_TIMESTAMP)
        ON CONFLICT (business_date)
        DO UPDATE SET last_sequence = daily_order_sequence.last_sequence + 1, updated_at = CURRENT_TIMESTAMP
        RETURNING last_sequence;
        """;
    Integer seq = jdbcTemplate.queryForObject(sql, Integer.class, businessDate);
    return seq != null ? seq : 1;
  }

  /**
   * Sincroniza a sequência diária quando um código foi gerado externamente pelo Hub (offline/local).
   * Garante que o contador do banco seja no mínimo o número recebido, prevenindo colisões futuras.
   */
  @Transactional
  public void syncSequence(LocalDate businessDate, int sequenceNumber) {
    if (sequenceNumber <= 0) {
      return;
    }
    String sql = """
        INSERT INTO daily_order_sequence (business_date, last_sequence, updated_at)
        VALUES (?, ?, CURRENT_TIMESTAMP)
        ON CONFLICT (business_date)
        DO UPDATE SET last_sequence = GREATEST(daily_order_sequence.last_sequence, ?), updated_at = CURRENT_TIMESTAMP;
        """;
    jdbcTemplate.update(sql, businessDate, sequenceNumber, sequenceNumber);
  }

  /**
   * Extrai o número sequencial de uma string de código (ex: "#0015" -> 15 ou "15" -> 15).
   */
  public Integer parseSequenceNumber(String orderCode) {
    if (orderCode == null || orderCode.isBlank()) {
      return null;
    }
    String digits = orderCode.replaceAll("[^0-9]", "");
    if (digits.isBlank()) {
      return null;
    }
    try {
      return Integer.parseInt(digits);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /**
   * Formata número sequencial no padrão de 4 dígitos com prefixo # (ex: 5 -> "#0005").
   */
  public String formatOrderCode(int sequence) {
    return String.format("#%04d", sequence);
  }
}
