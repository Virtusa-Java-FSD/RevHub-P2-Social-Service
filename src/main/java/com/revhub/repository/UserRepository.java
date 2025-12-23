package com.revhub.repository;
import com.revhub.model.User;
import com.revhub.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Boolean existsByEmail(String email);
    Boolean existsByUsername(String username);
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<User> searchUsers(@Param("query") String query);
    // Admin queries
    List<User> findByRole(UserRole role);
    List<User> findByIsBanned(Boolean isBanned);
    List<User> findByIsDeleted(Boolean isDeleted);
    @Query("SELECT u FROM User u WHERE u.isDeleted = false")
    List<User> findAllActive();
    long countByRole(UserRole role);
    long countByIsBanned(Boolean isBanned);
    long countByIsDeleted(Boolean isDeleted);
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startDate")
    long countUsersCreatedAfter(@Param("startDate") LocalDateTime startDate);
}
