package com.cardboardboxed.demo.reviews;
import com.cardboardboxed.demo.boardgames.BoardGameRank;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
//create repository for reviews
public interface ReviewRepository extends JpaRepository<Review, Integer> {
    List<Review> findByUserUsername(String username);

    List<Review> findByGameOrderByCreatedAtDesc(BoardGameRank game);

    List<Review> findTop5ByGameOrderByCreatedAtDesc(BoardGameRank game);

    long countByGame(BoardGameRank game);

    @Query("select avg(r.rating) from Review r where r.game = :game")
    Double findAverageRatingByGame(@Param("game") BoardGameRank game);
}
