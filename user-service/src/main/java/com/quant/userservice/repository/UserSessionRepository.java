package com.quant.userservice.repository;

import com.quant.userservice.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findBySessionTokenAndIsActiveTrue(String sessionToken);

    Optional<UserSession> findByRefreshToken(String refreshToken);

    List<UserSession> findByUserIdAndIsActiveTrue(Long userId);

    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.user.id = :userId")
    void deactivateAllUserSessions(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE UserSession s SET s.isActive = false WHERE s.expiresAt < :now")
    void deactivateExpiredSessions(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE UserSession s SET s.lastAccessedAt = :accessTime WHERE s.sessionToken = :token")
    void updateLastAccessTime(@Param("token") String token, @Param("accessTime") LocalDateTime accessTime);
}