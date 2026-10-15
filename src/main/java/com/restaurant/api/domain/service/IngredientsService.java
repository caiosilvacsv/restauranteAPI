package com.restaurant.api.domain.service;

import com.restaurant.api.domain.dto.IngredientRequest;
import com.restaurant.api.domain.dto.IngredientResponse;
import com.restaurant.api.domain.entity.Ingredient;
import com.restaurant.api.domain.exception.ConflictException;
import com.restaurant.api.domain.exception.NotFoundException;
import com.restaurant.api.domain.repository.IngredientRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IngredientsService {
  private final IngredientRepository ingredientRepository;
  public IngredientsService(IngredientRepository ingredientRepository) {
    this.ingredientRepository = ingredientRepository;
  }

  public IngredientResponse createIngredient(IngredientRequest request) {
    if (ingredientRepository.existsByName(request.name()))
      throw new ConflictException(
          "Ingredient exists",
          "Ingredient with name " + request.name() + " already exists");

    Ingredient ingredient = new Ingredient();

    ingredient.setName(request.name());
    ingredient.setPrice(request.price());
    ingredient.setActive(true);
    Ingredient saved= ingredientRepository.save(ingredient);
    return IngredientResponse.fromEntity(saved);
  }

  public IngredientResponse update(Long id, IngredientRequest request) {
    Ingredient ingredient = findById(id);

    ingredient.setName(request.name());
    ingredient.setPrice(request.price());
    ingredientRepository.save(ingredient);
    return IngredientResponse.fromEntity(ingredient);
  }

  public void deleteById(Long id) {
    Ingredient ingredient = findById(id);
    ingredient.setActive(false);
    ingredientRepository.save(ingredient);
  }

  public IngredientResponse activeIngredient(Long id) {
    Ingredient ingredient = findById(id);
    ingredient.setActive(true);
    Ingredient saved = ingredientRepository.save(ingredient);
    return IngredientResponse.fromEntity(saved);
  }

  public Page<IngredientResponse> findAll(Pageable pageable) {
     return ingredientRepository.
         findByActiveTrueOrderByNameAsc(pageable)
         .map(IngredientResponse::fromEntity);

  }
  public Page<IngredientResponse> findAllDelete(Pageable pageable) {
     return ingredientRepository.
         findByActiveFalseOrderByNameAsc(pageable)
         .map(IngredientResponse::fromEntity);
  }

  public List<IngredientResponse> saveAll(List<IngredientRequest> requests) {
    List<Ingredient> ingredients = requests
        .stream()
        .map(IngredientRequest::toEntity)
        .toList();

    List<Ingredient> saved = ingredientRepository.saveAll(ingredients);
    if(requests.size() !=  saved.size())
      throw new ConflictException("Ingredient not saved");

    return saved
        .stream()
        .map(IngredientResponse::fromEntity)
        .toList();
  }

  private Ingredient findById(Long id) {
    return ingredientRepository.findById(id).orElseThrow(
        () -> new NotFoundException("Product not found"));
  }
}

