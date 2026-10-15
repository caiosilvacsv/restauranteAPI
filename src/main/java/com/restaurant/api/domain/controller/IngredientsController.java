package com.restaurant.api.domain.controller;

import com.restaurant.api.domain.dto.IngredientRequest;
import com.restaurant.api.domain.dto.IngredientResponse;
import com.restaurant.api.domain.service.IngredientsService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RestController
@RequestMapping("/ingredient")
public class IngredientsController {
  private final IngredientsService service;

  public IngredientsController(IngredientsService service) {
    this.service = service;
  }

  //TODO : Modificar para entity response
  @PostMapping("/create")
  public IngredientResponse createIngredient(@RequestBody IngredientRequest request) {
    return service.createIngredient(request);
  }

  @PutMapping("/{id}")
  public IngredientResponse updateIngredient(@PathVariable Long id, @RequestBody IngredientRequest request) {
    return service.update(id, request);
  }

  @PutMapping("/{id}/delete")
  @ResponseStatus(HttpStatus.OK)
  public void delete(@PathVariable Long id) {
    service.deleteById(id);
  }

  @PutMapping("/{id}/active")
  public IngredientResponse activeIngredient(@PathVariable Long id) {
    return service.activeIngredient(id);
  }

  @GetMapping()
  public Page<IngredientResponse> findAll(Pageable page) {
    return service.findAll(page);
  }

  @GetMapping("/delete")
  public Page<IngredientResponse> findAllDelete(Pageable page) {
    return service.findAllDelete(page);
  }

  @PostMapping("/create-multiple")
  public List<IngredientResponse> saveAll(@RequestBody List<IngredientRequest> requests) {
    return service.saveAll(requests);
  }


}
