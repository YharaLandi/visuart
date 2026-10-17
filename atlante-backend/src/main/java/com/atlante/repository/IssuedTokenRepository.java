package com.atlante.repository;

import com.atlante.model.IssuedToken;
import com.atlante.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface IssuedTokenRepository extends JpaRepository<IssuedToken, Long> {

    Optional<IssuedToken> findByTokenHash(String tokenHash);

    @Query("select count(t) > 0 from IssuedToken t where t.tokenHash = :hash and t.revoked = false and t.expiresAt > :now")
    boolean isActive(@Param("hash") String hash, @Param("now") Instant now);

    @Modifying
    @Query("update IssuedToken t set t.revoked = true where t.user = :user and t.revoked = false")
    int revokeAllOf(@Param("user") User user);

    void deleteByUser(User user);

    @Modifying
    @Query("delete from IssuedToken t where t.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
