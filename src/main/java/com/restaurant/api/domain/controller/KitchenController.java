package com.restaurant.api.domain.controller;

import com.restaurant.api.domain.dto.KitchenItemResponse;
import com.restaurant.api.domain.service.KitchenService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/kitchen")
public class KitchenController {

  private final KitchenService service;

  public KitchenController(KitchenService service) {
    this.service = service;
  }

  @GetMapping("/item/peding")
  public List<KitchenItemResponse> ListItemsPending(){
    return service.ListAllKitchenItemsPending();
  }

  @GetMapping("/item/preparing")
  public List<KitchenItemResponse> ListItemsPreparing(){
    return service.ListAllKitchenItemsPreparing();
  }

  @GetMapping("/item/queued")
  public List<KitchenItemResponse> ListItemsQueued(){
    return service.ListAllKitchenItemsQueued();
  }

  @PatchMapping("/item/{itemId}/queued")
  @ResponseStatus(HttpStatus.OK)
  public KitchenItemResponse findItemById(@PathVariable Long itemId){
    return service.addToQueued(itemId);
  }

  @PatchMapping("/item/{itemId}/prepared")
  @ResponseStatus(HttpStatus.OK)
  public KitchenItemResponse initPreparedKitchen(@PathVariable Long itemId){
    return service.initPreparing(itemId);
  }

  @PatchMapping("/item/{itemId}/ready")
  @ResponseStatus(HttpStatus.OK)
  public KitchenItemResponse readyKitchen(@PathVariable Long itemId){
    return service.markReady(itemId);
  }

  @PatchMapping("/item/{itemId}/despatched")
  @ResponseStatus(HttpStatus.OK)
  public KitchenItemResponse initDespatchedKitchen(@PathVariable Long itemId){
    return service.initDespatched(itemId);
  }

  //FIXME - Move this section to the food delivery section.
  @PatchMapping("/item/{itemId}/delivered")
  @ResponseStatus(HttpStatus.OK)
  public KitchenItemResponse deliveredItem(@PathVariable Long itemId){
    return service.deliveredItem(itemId);
  }

}
