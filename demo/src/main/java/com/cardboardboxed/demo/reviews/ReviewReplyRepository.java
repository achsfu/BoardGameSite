package com.cardboardboxed.demo.reviews;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewReplyRepository
        extends JpaRepository<ReviewReply, Integer> {

    List<ReviewReply> findByReviewOrderByCreatedAtAsc(
            Review review
    );

    long countByReview(Review review);

    void deleteByReview(Review review);
}