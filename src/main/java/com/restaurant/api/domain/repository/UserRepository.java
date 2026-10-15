package com.restaurant.api.domain.repository;

import com.restaurant.api.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;

public interface UserRepository extends JpaRepository<User, Long> {
  User findByLogin(String login);
}
