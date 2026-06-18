package com.goaway.contexts.account.infrastructure.persistence;

import com.goaway.contexts.account.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    Optional<User> findByAppleUserId(String appleUserId);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);

    @Query("""
            SELECT u
            FROM User u
            WHERE (:query IS NULL OR :query = ''
                OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(COALESCE(u.displayName, '')) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY COALESCE(u.updatedAt, u.createdAt) DESC
            """)
    List<User> adminSearch(@Param("query") String query);
}
