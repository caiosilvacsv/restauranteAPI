package com.restaurant.api.domain.service;

import com.restaurant.api.domain.dto.GuestRequest;
import com.restaurant.api.domain.dto.UserLoginRequest;
import com.restaurant.api.domain.dto.UserRegisterRequest;
import com.restaurant.api.domain.dto.UserResponse;
import com.restaurant.api.domain.entity.User;
import com.restaurant.api.domain.enums.Role;
import com.restaurant.api.domain.exception.ConflictException;
import com.restaurant.api.domain.infra.security.TokenService;
import com.restaurant.api.domain.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {
  private final UserRepository userRepository;
  private final AuthenticationManager authenticationManager;
  private final TokenService tokenService;

  public AuthService(
      UserRepository userRepository,
      AuthenticationManager authenticationManager,
      TokenService tokenService
  ) {
    this.userRepository = userRepository;
    this.authenticationManager = authenticationManager;
    this.tokenService = tokenService;
  }

  public UserResponse login(UserLoginRequest request){
    var userNamePassword = new UsernamePasswordAuthenticationToken(request.login(), request.password());
    var auth = this.authenticationManager.authenticate(userNamePassword);
    if(!auth.isAuthenticated()) throw new ConflictException("Error in User ", "Invalid username and password");
    return UserResponse.fromEntity(
        (User) auth.getPrincipal(),
        tokenService.generateToken((User) auth.getPrincipal()));
  }

  public UserResponse register(UserRegisterRequest request){
    if(this.userRepository.findByLogin(request.login()) != null)
      throw new ConflictException("Error in User ", "Error in register user!");

    String passwordEncrypted = new BCryptPasswordEncoder().encode(request.password());
    User user = new User();
    user.setLogin(request.login());
    user.setPassword(passwordEncrypted);
    user.setName(request.name());
    user.setNumber(request.number());
    user.setActive(true);
    user.setRole(Role.USER);

    User saved = userRepository.save(user);

    String token = tokenService.generateToken(saved);

    return UserResponse.fromEntity(saved, token);
  }

  public UserResponse loginGuest(GuestRequest request){
    String guestLogin = "GUEST-" + request.number();

    User user = userRepository.findByLogin(guestLogin);
    User finalGuest;

    if (user != null) {
      String novoNome = request.name() != null ? request.name().trim() : "";
      boolean temNovoNomeValido = !novoNome.isBlank() && !novoNome.equalsIgnoreCase("Cliente");
      boolean nomeAtualEhGenerico = user.getName() == null || user.getName().isBlank() || user.getName().equalsIgnoreCase("Cliente");

      if (temNovoNomeValido && (nomeAtualEhGenerico || !novoNome.equalsIgnoreCase(user.getName()))) {
        user.setName(novoNome);
        userRepository.save(user);
      }
      finalGuest = user;
    } else {
      finalGuest = createGuest(guestLogin, request);
    }

    String token = tokenService.generateToken(finalGuest);
    return UserResponse.fromEntity(finalGuest, token);
  }

  private User createGuest(String guestLogin, GuestRequest request){
    User guest = new User();
    guest.setLogin(guestLogin);
    guest.setPassword(
        new BCryptPasswordEncoder()
            .encode(
                UUID.randomUUID().toString() +
                guestLogin));
    guest.setNumber(request.number());
    String nome = request.name() != null ? request.name().trim() : "";
    if (nome.isBlank() || nome.equalsIgnoreCase("Cliente")) {
      guest.setName(null);
    } else {
      guest.setName(nome);
    }
    guest.setRole(Role.CUSTOMER);
    guest.setActive(true);
    return userRepository.save(guest);
  }
}


