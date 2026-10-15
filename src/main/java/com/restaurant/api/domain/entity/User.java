package com.restaurant.api.domain.entity;

import com.restaurant.api.domain.enums.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User implements UserDetails {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;
  @Column(nullable = false, unique = true)
  private String login;
  @Column(nullable = false)
  private String password;
  @Column(nullable = false)
  private String number;
  @Column(nullable = false)
  private String name;
  @Enumerated(EnumType.STRING)
  private Role role = Role.USER;
  private Boolean active = true;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    if (this.role == Role.ADMIN)
      return List.of(
          new SimpleGrantedAuthority("ROLE_ADMIN"),
          new SimpleGrantedAuthority("ROLE_WAITER"),
          new SimpleGrantedAuthority("ROLE_USER"));
    if (this.role == Role.WAITER)
      return List.of(
          new SimpleGrantedAuthority("ROLE_WAITER"),
          new SimpleGrantedAuthority("ROLE_USER"));
    if (this.role == Role.CUSTOMER)
      return List.of(
          new SimpleGrantedAuthority("ROLE_CUSTOMER"),
          new SimpleGrantedAuthority("ROLE_USER"));
    return List.of(new SimpleGrantedAuthority("ROLE_USER"));
  }

  /**
   * Returns the username used to authenticate the user. Cannot return
   * <code>null</code>.
   *
   * @return the username (never <code>null</code>)
   */
  @Override
  public String getUsername() {
    return this.login;
  }

  @Override
  public boolean isAccountNonExpired() {
    return UserDetails.super.isAccountNonExpired();
  }

  @Override
  public boolean isAccountNonLocked() {
    return UserDetails.super.isAccountNonLocked();
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return UserDetails.super.isCredentialsNonExpired();
  }

  @Override
  public boolean isEnabled() {
    return Boolean.TRUE.equals(this.active);
  }

  @Override
  public String toString() {
    return "User{"
    + "\n\tid :" + this.id
    + "\n\tlogin :" + this.login
    + "\n\tnumber :" + this.number
    + "\n\tname :" + this.name
    + "\n\trole :" + this.role;
  }
}
