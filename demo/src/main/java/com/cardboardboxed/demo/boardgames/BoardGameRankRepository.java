package com.cardboardboxed.demo.boardgames;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardGameRankRepository
        extends JpaRepository<BoardGameRank, Integer> {

    /*
     * Returns the first non-expansion game matching the title.
     * Using "findFirst" prevents an error if duplicate titles exist.
     */
    Optional<BoardGameRank> findFirstByTitleIgnoreCaseOrderByRankPositionAsc(
            String title
    );

    List<BoardGameRank>
    findTop12ByTitleContainingIgnoreCaseOrderByRankPositionAsc(
            String title
    );

    /*
     * The highest-ranked non-expansion games that have images.
     */
    @Query("""
        select game
        from BoardGameRank game
                                where game.imageUrl is not null
          and game.imageUrl <> ''
        order by
          case when game.rankPosition is null then 1 else 0 end,
          case when game.rankPosition = 0 then 1 else 0 end,
          game.rankPosition asc
        """)
    List<BoardGameRank> findHomepagePopularGames(
            Pageable pageable
    );

    /*
     * Games with the highest community score that have images.
     */
    @Query("""
        select game
        from BoardGameRank game
                                where game.imageUrl is not null
          and game.imageUrl <> ''
          and game.communityScore is not null
        order by game.communityScore desc, game.rankPosition asc
        """)
    List<BoardGameRank> findHighestRatedGames(
            Pageable pageable
    );

    /*
     * Finds games that have received reviews.
     */
    @Query("""
        select distinct game
        from BoardGameRank game
        join game.reviews review
        where game.imageUrl is not null
          and game.imageUrl <> ''
        order by game.rankPosition asc
        """)
    List<BoardGameRank> findReviewedGames(
            Pageable pageable
    );

    @Query("""
        select game
        from BoardGameRank game
        where lower(game.title) like lower(concat('%', :query, '%'))
        order by
          case when game.rankPosition = 0 then 1 else 0 end asc,
          game.rankPosition asc nulls last,
          game.title asc
        """)
    Page<BoardGameRank> searchSimilarGames(
            @Param("query") String query,
            Pageable pageable
    );
}