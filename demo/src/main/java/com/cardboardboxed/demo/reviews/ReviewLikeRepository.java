package com.cardboardboxed.demo.reviews;

import com.cardboardboxed.demo.useracounts.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewLikeRepository
        extends JpaRepository<ReviewLike, Integer> {

    /*
     * Finds a like belonging to a specific user and review.
     * Used when checking whether the current user already liked it.
     */
    Optional<ReviewLike> findByReviewAndUser(
            Review review,
            User user
    );

    /*
     * Counts all likes attached to one review.
     */
    long countByReview(Review review);

    /*
     * Deletes all like records belonging to a review.
     * This will be useful when a review is deleted.
     */
    void deleteByReview(Review review);

    /*
     * Checks whether a user has already liked a review.
     */
    boolean existsByReviewAndUser(
            Review review,
            User user
    );
}