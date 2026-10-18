package com.restaurant.api.domain.service;

import com.restaurant.api.domain.dto.PizzaSizeRequest;
import com.restaurant.api.domain.dto.PizzaSizeResponse;
import com.restaurant.api.domain.entity.PizzaSize;
import com.restaurant.api.domain.exception.BusinessRulesException;
import com.restaurant.api.domain.exception.NotFoundException;
import com.restaurant.api.domain.repository.PizzaSizeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PizzaSizeService {

  private final PizzaSizeRepository pizzaSizeRepository;

  public PizzaSizeService(PizzaSizeRepository pizzaSizeRepository) {
    this.pizzaSizeRepository = pizzaSizeRepository;
  }

  @org.springframework.cache.annotation.Cacheable(value = "sizes", key = "'active'")
  public List<PizzaSizeResponse> findAllActive() {
    return pizzaSizeRepository.findByActiveTrue()
        .stream()
        .map(PizzaSizeResponse::fromEntity)
        .toList();
  }

  public List<PizzaSizeResponse> findAll() {
    return pizzaSizeRepository.findAll()
        .stream()
        .map(PizzaSizeResponse::fromEntity)
        .toList();
  }

  public PizzaSizeResponse findById(Long id) {
    PizzaSize size = findEntityById(id);
    return PizzaSizeResponse.fromEntity(size);
  }

  public PizzaSize findEntityById(Long id) {
    return pizzaSizeRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Tamanho de pizza não encontrado", "ID do tamanho: " + id));
  }

  @org.springframework.cache.annotation.CacheEvict(value = "sizes", allEntries = true)
  @Transactional
  public PizzaSizeResponse create(PizzaSizeRequest request) {
    pizzaSizeRepository.findByShortNameIgnoreCase(request.shortName())
        .ifPresent(existing -> {
          throw new BusinessRulesException("Nome curto já cadastrado", "O nome curto '" + request.shortName() + "' já está em uso.");
        });

    PizzaSize size = new PizzaSize();
    size.setName(request.name());
    size.setShortName(request.shortName().trim().toUpperCase());
    size.setSlices(request.slices());
    size.setMaxFlavors(request.maxFlavors() != null ? request.maxFlavors() : 1);
    size.setPrice(request.price() != null ? request.price() : java.math.BigDecimal.ZERO);
    size.setActive(request.active() != null ? request.active() : true);

    return PizzaSizeResponse.fromEntity(pizzaSizeRepository.save(size));
  }

  @org.springframework.cache.annotation.CacheEvict(value = "sizes", allEntries = true)
  @Transactional
  public PizzaSizeResponse update(Long id, PizzaSizeRequest request) {
    PizzaSize size = findEntityById(id);

    pizzaSizeRepository.findByShortNameIgnoreCase(request.shortName())
        .filter(existing -> !existing.getId().equals(id))
        .ifPresent(existing -> {
          throw new BusinessRulesException("Nome curto já cadastrado", "O nome curto '" + request.shortName() + "' já está em uso por outro tamanho.");
        });

    size.setName(request.name());
    size.setShortName(request.shortName().trim().toUpperCase());
    size.setSlices(request.slices());
    size.setMaxFlavors(request.maxFlavors() != null ? request.maxFlavors() : 1);
    if (request.price() != null) {
      size.setPrice(request.price());
    }
    if (request.active() != null) {
      size.setActive(request.active());
    }

    return PizzaSizeResponse.fromEntity(pizzaSizeRepository.save(size));
  }

  @org.springframework.cache.annotation.CacheEvict(value = "sizes", allEntries = true)
  @Transactional
  public void delete(Long id) {
    PizzaSize size = findEntityById(id);
    size.setActive(false);
    pizzaSizeRepository.save(size);
  }
}
