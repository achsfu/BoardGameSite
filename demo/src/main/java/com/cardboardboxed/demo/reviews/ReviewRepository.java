package com.cardboardboxed.demo.reviews;

import com.cardboardboxed.demo.boardgames.BoardGameRank;
import com.cardboardboxed.demo.useracounts.User;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Integer> {

    List<Review> findByUserUsername(String username);

    List<Review> findByGameOrderByCreatedAtDesc(
            BoardGameRank game
    );

    List<Review> findTop5ByGameOrderByCreatedAtDesc(
            BoardGameRank game
    );

    long countByGame(BoardGameRank game);

    @Query("""
        select avg(r.rating)
        from Review r
        where r.game = :game
        """)
    Double findAverageRatingByGame(
            @Param("game") BoardGameRank game
    );

    @Query("""
        select r
        from Review r
        join fetch r.user u
        where u in (
            select uf.followed
            from UserFollow uf
            where uf.follower = :follower
        )
        order by r.createdAt desc
        """)
    List<Review> findFeedReviewsForUser(
            @Param("follower") User follower
    );

    /*
     * Recent reviews displayed on the public homepage.
     */
    @Query("""
        select r
        from Review r
        join fetch r.game g
        left join fetch r.user
        order by r.createdAt desc
        """)
    List<Review> findRecentHomepageReviews(
            Pageable pageable
    );

    @Query("""
        select r
        from Review r
        left join r.user u
        where (
            :keyword = ''
            or lower(coalesce(r.gameTitle, '')) like lower(concat('%', :keyword, '%'))
            or lower(coalesce(r.reviewText, '')) like lower(concat('%', :keyword, '%'))
            or lower(coalesce(u.username, '')) like lower(concat('%', :keyword, '%'))
        )
        and (:rating is null or r.rating = :rating)
        order by r.createdAt desc
        """)
    List<Review> findForModeration(
            @Param("keyword") String keyword,
            @Param("rating") Integer rating
    );
}