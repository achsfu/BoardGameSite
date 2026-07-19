package com.cardboardboxed.demo.reviews;

import com.cardboardboxed.demo.boardgames.BoardGameRank;
import com.cardboardboxed.demo.useracounts.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// Repository for board-game reviews
public interface ReviewRepository extends JpaRepository<Review, Integer> {

    List<Review> findByUserUsername(String username);

    List<Review> findByGameOrderByCreatedAtDesc(BoardGameRank game);

    List<Review> findTop5ByGameOrderByCreatedAtDesc(BoardGameRank game);

    long countByGame(BoardGameRank game);

    @Query("select avg(r.rating) from Review r where r.game = :game")
    Double findAverageRatingByGame(@Param("game") BoardGameRank game);

    /*
     * Returns reviews written by users followed by the current user.
     * Reviews are ordered from newest to oldest.
     */
    @Query("""
        SELECT r
        FROM Review r
        WHERE r.user IN (
            SELECT uf.followed
            FROM UserFollow uf
            WHERE uf.follower = :follower
        )
        ORDER BY r.createdAt DESC
        """)
    List<Review> findFeedReviewsForUser(
            @Param("follower") User follower
    );
}