package com.numbergame.gamenumber.repository;

import com.numbergame.gamenumber.entity.GameHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GameHistoryRepository extends JpaRepository<GameHistory, Long> {

    List<GameHistory> findByUserIdOrderByPlayedAtDesc(Long userId);

    @Query("SELECT COUNT(gh) FROM GameHistory gh WHERE gh.userId = :userId AND gh.isCorrect = true")
    Long countCorrectGuessesByUserId(@Param("userId") Long userId);

    @Query("SELECT gh FROM GameHistory gh WHERE gh.userId = :userId AND gh.playedAt >= :since ORDER BY gh.playedAt DESC")
    List<GameHistory> findRecentGamesByUserId(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}

