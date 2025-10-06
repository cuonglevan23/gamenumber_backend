package com.numbergame.gamenumber.repository;

import com.numbergame.gamenumber.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // Pessimistic locking for concurrent /guess requests
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithLock(@Param("id") Long id);

    // Optimized query for leaderboard with index hint
    @Query(value = "SELECT * FROM users ORDER BY score DESC, username ASC LIMIT 10", nativeQuery = true)
    List<User> findTop10ByOrderByScoreDesc();

    // Get top users by score for leaderboard with custom limit - using Spring Data naming
    @Query(value = "SELECT u FROM User u ORDER BY u.score DESC")
    List<User> findAllByOrderByScoreDesc();

    // Count users with score greater than
    @Query("SELECT COUNT(u) FROM User u WHERE u.score > :score")
    Long countUsersWithScoreGreaterThan(@Param("score") Integer score);
}
