package com.quant.userservice.repository;

import com.quant.userservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.username = :usernameOrEmail OR u.email = :usernameOrEmail")
    Optional<User> findByUsernameOrEmail(@Param("usernameOrEmail") String usernameOrEmail);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findByStatus(User.UserStatus status);

    List<User> findByRole(User.UserRole role);

    @Query("SELECT u FROM User u WHERE u.status = :status AND u.enableTrading = true")
    List<User> findActiveTradingUsers(@Param("status") User.UserStatus status);
}