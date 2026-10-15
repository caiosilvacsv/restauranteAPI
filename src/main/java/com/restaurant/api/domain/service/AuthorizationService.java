package com.restaurant.api.domain.service;

import com.restaurant.api.domain.entity.User;
import com.restaurant.api.domain.exception.ConflictException;
import com.restaurant.api.domain.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService implements UserDetailsService {

  @Autowired
  UserRepository repository;
  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    User user = repository.findByLogin(username);
    if (user == null) {
      throw new UsernameNotFoundException("Not found");
    }
    return user;
  }
}