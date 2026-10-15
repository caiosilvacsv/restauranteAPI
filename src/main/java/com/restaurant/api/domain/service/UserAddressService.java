package com.restaurant.api.domain.service;

import com.restaurant.api.domain.dto.UserAddressRequest;
import com.restaurant.api.domain.dto.UserAddressResponse;
import com.restaurant.api.domain.entity.User;
import com.restaurant.api.domain.entity.UserAddress;
import com.restaurant.api.domain.exception.BusinessRulesException;
import com.restaurant.api.domain.exception.NotFoundException;
import com.restaurant.api.domain.repository.UserAddressRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserAddressService {

  private final UserAddressRepository userAddressRepository;

  public UserAddressService(UserAddressRepository userAddressRepository) {
    this.userAddressRepository = userAddressRepository;
  }

  private User getAuthenticatedUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof User user)) {
      throw new BusinessRulesException("Usuário não autenticado", "É necessário estar autenticado para gerenciar endereços.");
    }
    return user;
  }

  public List<UserAddressResponse> findAllForCurrentUser() {
    User user = getAuthenticatedUser();
    return userAddressRepository.findByUserIdAndActiveTrueOrderByIsDefaultDescIdDesc(user.getId())
        .stream()
        .map(UserAddressResponse::fromEntity)
        .toList();
  }

  public UserAddressResponse findByIdForCurrentUser(Long id) {
    User user = getAuthenticatedUser();
    UserAddress address = userAddressRepository.findByIdAndUserIdAndActiveTrue(id, user.getId())
        .orElseThrow(() -> new NotFoundException("Endereço não encontrado", "ID do endereço: " + id));
    return UserAddressResponse.fromEntity(address);
  }

  public UserAddress findEntityByIdForCurrentUser(Long id, Long userId) {
    return userAddressRepository.findByIdAndUserIdAndActiveTrue(id, userId)
        .orElseThrow(() -> new NotFoundException("Endereço não encontrado", "ID do endereço: " + id));
  }

  @Transactional
  public UserAddressResponse create(UserAddressRequest request) {
    User user = getAuthenticatedUser();

    List<UserAddress> existing = userAddressRepository.findByUserIdAndActiveTrueOrderByIsDefaultDescIdDesc(user.getId());
    boolean shouldBeDefault = existing.isEmpty() || Boolean.TRUE.equals(request.isDefault());

    if (shouldBeDefault) {
      userAddressRepository.resetDefaultAddresses(user.getId());
    }

    UserAddress address = new UserAddress();
    address.setUser(user);
    address.setNickname(request.nickname());
    address.setStreet(request.street());
    address.setNumber(request.number());
    address.setNeighborhood(request.neighborhood());
    address.setComplement(request.complement());
    address.setReferencePoint(request.referencePoint());
    address.setIsDefault(shouldBeDefault);
    address.setActive(true);

    return UserAddressResponse.fromEntity(userAddressRepository.save(address));
  }

  @Transactional
  public UserAddressResponse update(Long id, UserAddressRequest request) {
    User user = getAuthenticatedUser();
    UserAddress address = userAddressRepository.findByIdAndUserIdAndActiveTrue(id, user.getId())
        .orElseThrow(() -> new NotFoundException("Endereço não encontrado", "ID do endereço: " + id));

    if (Boolean.TRUE.equals(request.isDefault()) && !address.getIsDefault()) {
      userAddressRepository.resetDefaultAddresses(user.getId());
      address.setIsDefault(true);
    }

    address.setNickname(request.nickname());
    address.setStreet(request.street());
    address.setNumber(request.number());
    address.setNeighborhood(request.neighborhood());
    address.setComplement(request.complement());
    address.setReferencePoint(request.referencePoint());

    return UserAddressResponse.fromEntity(userAddressRepository.save(address));
  }

  @Transactional
  public UserAddressResponse setDefault(Long id) {
    User user = getAuthenticatedUser();
    UserAddress address = userAddressRepository.findByIdAndUserIdAndActiveTrue(id, user.getId())
        .orElseThrow(() -> new NotFoundException("Endereço não encontrado", "ID do endereço: " + id));

    userAddressRepository.resetDefaultAddresses(user.getId());
    address.setIsDefault(true);
    return UserAddressResponse.fromEntity(userAddressRepository.save(address));
  }

  @Transactional
  public void delete(Long id) {
    User user = getAuthenticatedUser();
    UserAddress address = userAddressRepository.findByIdAndUserIdAndActiveTrue(id, user.getId())
        .orElseThrow(() -> new NotFoundException("Endereço não encontrado", "ID do endereço: " + id));

    address.setActive(false);
    address.setIsDefault(false);
    userAddressRepository.save(address);
  }
}
