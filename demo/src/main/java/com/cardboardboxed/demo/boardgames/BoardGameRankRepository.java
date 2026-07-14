package com.cardboardboxed.demo.boardgames;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardGameRankRepository extends JpaRepository<BoardGameRank, Integer> {

    BoardGameRank findByTitleIgnoreCase(String title);

    List<BoardGameRank> findAllByIsExpansionOrderByRankPositionAsc(Boolean isExpansion);

    List<BoardGameRank> findTop12ByTitleContainingIgnoreCaseOrderByRankPositionAsc(String title);

    @Query("""
            select game
            from BoardGameRank game
            where lower(game.title) like lower(concat('%', :query, '%'))
              and game.isExpansion = false
            order by
              case when game.rankPosition = 0 then 1 else 0 end asc,
              game.rankPosition asc nulls last,
              game.title asc
            """)
    Page<BoardGameRank> searchSimilarGames(@Param("query") String query, Pageable pageable);
}