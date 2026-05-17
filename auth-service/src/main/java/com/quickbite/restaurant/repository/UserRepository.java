package com.quickbite.restaurant.repository;

import com.quickbite.restaurant.entity.User;
import com.quickbite.restaurant.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByPhone(String phone);
    List<User> findAllByRole(Role role);
    List<User> findByFullNameContaining(String name);
    Optional<User> findByRefreshToken(String refreshToken);
    Optional<User> findByPasswordResetToken(String passwordResetToken);
    void deleteById(Long id);
}
