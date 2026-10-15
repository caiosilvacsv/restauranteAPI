package com.restaurant.api.domain.controller;

import com.restaurant.api.domain.dto.KitchenItemResponse;
import com.restaurant.api.domain.service.KitchenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/kitchen")
@Tag(name = "KDS / Cozinha", description = "Endpoints para visualização de itens da cozinha e transição de etapas de produção")
public class KitchenController {

  private final KitchenService service;

  public KitchenController(KitchenService service) {
    this.service = service;
  }

  @GetMapping({"/item/pending", "/item/peding"})
  @Operation(summary = "Listar itens pendentes de preparo", description = "Suporta tanto /item/pending (correto) quanto /item/peding por retrocompatibilidade")
  public ResponseEntity<List<KitchenItemResponse>> listItemsPending() {
    return ResponseEntity.ok(service.listAllKitchenItemsPending());
  }

  @GetMapping("/item/preparing")
  @Operation(summary = "Listar itens em preparo")
  public ResponseEntity<List<KitchenItemResponse>> listItemsPreparing() {
    return ResponseEntity.ok(service.listAllKitchenItemsPreparing());
  }

  @GetMapping("/item/queued")
  @Operation(summary = "Listar itens na esteira/fila de produção")
  public ResponseEntity<List<KitchenItemResponse>> listItemsQueued() {
    return ResponseEntity.ok(service.listAllKitchenItemsQueued());
  }

  @PatchMapping("/item/{itemId}/queued")
  @Operation(summary = "Enfileirar item na esteira de produção (idempotente)")
  public ResponseEntity<KitchenItemResponse> queueItem(@PathVariable Long itemId) {
    return ResponseEntity.ok(service.addToQueued(itemId));
  }

  @PatchMapping("/item/{itemId}/prepared")
  @Operation(summary = "Iniciar preparação do item no forno/bancada (idempotente)")
  public ResponseEntity<KitchenItemResponse> initPreparing(@PathVariable Long itemId) {
    return ResponseEntity.ok(service.initPreparing(itemId));
  }

  @PatchMapping("/item/{itemId}/ready")
  @Operation(summary = "Marcar item como pronto para entrega/expedição (idempotente)")
  public ResponseEntity<KitchenItemResponse> markReady(@PathVariable Long itemId) {
    return ResponseEntity.ok(service.markReady(itemId));
  }

  @PatchMapping("/item/{itemId}/despatched")
  @Operation(summary = "Marcar item como despachado (idempotente)")
  public ResponseEntity<KitchenItemResponse> initDespatched(@PathVariable Long itemId) {
    return ResponseEntity.ok(service.initDespatched(itemId));
  }

  @PatchMapping("/item/{itemId}/delivered")
  @Operation(summary = "Marcar item como entregue à mesa ou ao cliente (idempotente)")
  public ResponseEntity<KitchenItemResponse> deliveredItem(@PathVariable Long itemId) {
    return ResponseEntity.ok(service.deliveredItem(itemId));
  }
}
