package com.restaurant.api.domain.repository;

import com.restaurant.api.domain.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

  List<UserAddress> findByUserIdAndActiveTrueOrderByIsDefaultDescIdDesc(Long userId);

  Optional<UserAddress> findByIdAndUserIdAndActiveTrue(Long id, Long userId);

  Optional<UserAddress> findByUserIdAndIsDefaultTrueAndActiveTrue(Long userId);

  @Modifying
  @Query("UPDATE UserAddress a SET a.isDefault = false WHERE a.user.id = :userId")
  void resetDefaultAddresses(Long userId);
}
