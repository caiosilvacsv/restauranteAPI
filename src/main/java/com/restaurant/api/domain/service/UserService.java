package com.restaurant.api.domain.service;

import com.restaurant.api.domain.dto.StaffRegisterRequest;
import com.restaurant.api.domain.dto.UserResponse;
import com.restaurant.api.domain.entity.User;
import com.restaurant.api.domain.enums.Role;
import com.restaurant.api.domain.exception.BusinessRulesException;
import com.restaurant.api.domain.exception.ConflictException;
import com.restaurant.api.domain.exception.NotFoundException;
import com.restaurant.api.domain.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public UserResponse createStaff(StaffRegisterRequest request) {
    if (request.role() != Role.ADMIN && request.role() != Role.WAITER) {
      throw new BusinessRulesException(
          "Cargo inválido",
          "O cadastro de equipe permite apenas os cargos ADMIN ou WAITER."
      );
    }

    if (userRepository.existsByLogin(request.login())) {
      throw new ConflictException(
          "Login indisponível",
          "Já existe um usuário cadastrado com este login."
      );
    }

    User staff = new User();
    staff.setLogin(request.login());
    staff.setPassword(passwordEncoder.encode(request.password()));
    staff.setName(request.name());
    staff.setNumber(request.number());
    staff.setRole(request.role());
    staff.setActive(true);

    User saved = userRepository.save(staff);
    return UserResponse.fromEntity(saved);
  }

  @Transactional(readOnly = true)
  public List<UserResponse> listStaff() {
    return userRepository.findByRoleIn(List.of(Role.ADMIN, Role.WAITER))
        .stream()
        .map(UserResponse::fromEntity)
        .toList();
  }

  @Transactional
  public UserResponse toggleActive(Long id) {
    User user = userRepository.findById(id).orElseThrow(
        () -> new NotFoundException("Usuário não encontrado", "Não foi encontrado usuário com ID: " + id)
    );

    user.setActive(!Boolean.TRUE.equals(user.getActive()));
    User updated = userRepository.save(user);
    return UserResponse.fromEntity(updated);
  }
}
